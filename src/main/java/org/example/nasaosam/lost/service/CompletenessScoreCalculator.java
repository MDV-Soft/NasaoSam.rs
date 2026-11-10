package org.example.nasaosam.lost.service;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.lost.entity.LostReport;
import org.example.nasaosam.lost.entity.LostReportItem;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Racuna ocenu kompletnosti oglasa, od 0 do 100.
 *
 * CEMU SLUZI
 *   1. Frontend je prikazuje kao traku napretka ("Opis je 60% potpun") i podstice
 *      vlasnika da doda jos detalja. Sto je oglas detaljniji, to spajanje bolje radi.
 *   2. Cuva se u bazi, pa kasnije moze da vidis da li kompletniji oglasi zaista
 *      dovode do vracenog  predmeta i da prema tome podesi tezine.
 *
 * KAKO SE RACUNA
 *   - do 30 bodova za ceo oglas: opis mesta, tacka na mapi, privatni detalji (po 10),
 *   - do 70 bodova za predmete: svaki predmet dobija ocenu 0-100, uzima se prosek
 *     svih predmeta i množi sa 0,7.
 *   Prosek (a ne zbir) da oglas sa tri predmeta ne bi automatski imao vecu ocenu
 *   od oglasa sa jednim, jednako dobro opisanim predmetom.
 *
 *  Component ti Vojco znaci da je Spring pravi jednom i ubacuje u servis kroz konstruktor.
 * Klasa nema stanje pa je bezbedna za istovremenu upotrebu iz vise poziva
 */

@Component
public class CompletenessScoreCalculator {
    // Bodovi za ceo oglas (zbir najviše 30)
    private static final int POINTS_PLACE_DESCRIPTION = 10;
    private static final int POINTS_COORDINATES = 10;
    private static final int POINTS_PRIVATE_DETAILS = 10;

    // Koliko najvise bodova od ukupnih 100 nose predmeti
    private static final int ITEMS_MAX_POINTS = 70;

    // Bodovi za predmete koji nisu dokumenta (zbir najviše 100)
    private static final int POINTS_COLOR = 15;
    private static final int POINTS_BRAND = 15;
    private static final int POINTS_MATERIAL = 10;
    private static final int POINTS_DISTINCTIVE_MARKS = 20;
    private static final int POINTS_DESCRIPTION_SHORT = 20;   // 40+ znakova
    private static final int POINTS_DESCRIPTION_MEDIUM = 30;  // 100+ znakova
    private static final int POINTS_DESCRIPTION_LONG = 40;    // 200+ znakova

    // Bodovi za dokumenta (zbir najviše 100)
    private static final int POINTS_DOCUMENT_TYPE = 30;
    private static final int POINTS_NAME_HASH = 40;
    private static final int POINTS_NAME_DOB_HASH = 30;

    /**
     * Glavna metoda: racuna ocenu celog oglasa.
     *
     * KORACI
     *   1. Saberu se bodovi za polja oglasa (najvise 30).
     *   2. Ako oglas nema predmete, vracaju se samo ti bodovi.
     *
     *   3. Izracuna se ocena svakog predmeta i njihov prosek.
     *   4. Prosek se pomnozi sa 0,7 i zaokruzi na ceo broj.
     *   5. Saberu se oba dela; rezultat se ogranic na 100 kao zastita
     *      ako neko kasnije promeni tezine pa bude xbir preko 100.
     *
     * PRIMER
     *   Oglas sa opisom mesta (10) i jednim novacnikom ocene 80:
     *   10 + round(80 * 0.7) = 10 + 56 = 66
     *
     * Metoda samo citat entitet i nista ne menja; rezultat postavlja servis.
     */


    public int calculate(LostReport report) {
        int reportPoints;
        int itemsTotal;
        int itemCount;
        int denominator;
        int itemsPoints;
        int total;
        List<LostReportItem> items;

        // Korak 1: bodovi za polja oglasa
        reportPoints = 0;
        if (report.getPlaceDescription() != null) {
            reportPoints = reportPoints + POINTS_PLACE_DESCRIPTION;
        }
        // tack na mapi vazi samo ako postoje obe koordinate
        if (report.getLatitude() != null && report.getLongitude() != null) {
            reportPoints = reportPoints + POINTS_COORDINATES;
        }
        if (report.getPrivateDetails() != null) {
            reportPoints = reportPoints + POINTS_PRIVATE_DETAILS;
        }

        // Korak 2: zastita od deljenja nulom
        items = report.getItems();
        if (items.isEmpty()) {
            return reportPoints;
        }

        // Korak 3: zbir ocena svih predmeta
        itemsTotal = 0;
        for (int index = 0; index < items.size(); index++) {
            itemsTotal = itemsTotal + calculateItemScore(items.get(index));
        }

        itemCount = items.size();
        denominator = itemCount * 100;
        itemsPoints = (itemsTotal * ITEMS_MAX_POINTS + denominator / 2) / denominator;

        // Korak 5: ukupno, ograniceno na 100
        total = reportPoints + itemsPoints;
        if (total > 100) {
            total = 100;
        }
        return total;
    }

    /**
     * Racuna ocenu jednog predmeta (0-100).
     *
     * Dokumenta i ostali predmeti se ocenjuju razlicito jer kod njih
     * spajanju pomazu druge stvari kao nrp ::
     *   - dokument: identitet vlasnika (hesevi imena),
     *   - ostalo: izgled i opis.
     *
     * Bez modifikatora pristupa (package-private): vidljiva je samo klasama iz istog paketa,
     * pa je test (koji je u istom paketu) moze pozvati direktno, a ostatak aplikacije ne.
     */
    int calculateItemScore(LostReportItem item) {
        if (item.getCategory() == ItemCategory.DOCUMENT) {
            return calculateDocumentScore(item);
        }
        return calculateObjectScore(item);
    }

    /**
     * Ocena dokumenta.
     *
     *   vrsta dokumenta      30
     *   hesh imena            40   (najvaznije)
     *   hesh imena + datuma   30   (skoro jedinstven kljuc, resava problem istih imena)
     *
     * Boja, marka i opis se ovde ne boduju, jer sve licne karte izgledaju isto
     */
    private int calculateDocumentScore(LostReportItem item) {
        int score;

        score = 0;
        if (item.getDocumentType() != null) {
            score = score + POINTS_DOCUMENT_TYPE;
        }
        if (item.getNameHash() != null) {
            score = score + POINTS_NAME_HASH;
        }
        if (item.getNameDobHash() != null) {
            score = score + POINTS_NAME_DOB_HASH;
        }
        return score;
    }

    /**
     * Ocena predmeta koji nije dokument
     *
     *   boja              15
     *   marka             15
     *   materijal         10
     *   posebni znaci     20   (jak signal: nalepnica neka ii nesto  je skoro jedinstvena)
     *   opis              do 40, stepenasto:
     *                        40+ znakova   -> 20  (minimum programski)
     *                        100+ znakova  -> 30
     *                        200+ znakova  -> 40
     *
     *
     *
     * Ovo meri samo DUZINU< NE KVALITET OPSIA TO CE KASNIJE AI DA PROCENI
     */
    private int calculateObjectScore(LostReportItem item) {
        int score;
        int descriptionLength;

        score = 0;
        if (item.getColor() != null) {
            score = score + POINTS_COLOR;
        }
        if (item.getBrand() != null) {
            score = score + POINTS_BRAND;
        }
        if (item.getMaterial() != null) {
            score = score + POINTS_MATERIAL;
        }
        if (item.getDistinctiveMarks() != null) {
            score = score + POINTS_DISTINCTIVE_MARKS;
        }

        descriptionLength = 0;
        if (item.getDescription() != null) {
            descriptionLength = item.getDescription().length();
        }

        if (descriptionLength >= 200) {
            score = score + POINTS_DESCRIPTION_LONG;
        } else if (descriptionLength >= 100) {
            score = score + POINTS_DESCRIPTION_MEDIUM;
        } else if (descriptionLength >= 40) {
            score = score + POINTS_DESCRIPTION_SHORT;
        }

        return score;
    }
}
