package org.example.nasaosam.lost.service;

import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.security.HmacService;
import org.example.nasaosam.lost.dto.CreateLostReportRequest;
import org.example.nasaosam.lost.dto.LostReportItemRequest;
import org.example.nasaosam.lost.dto.LostReportItemResponse;
import org.example.nasaosam.lost.dto.LostReportResponse;
import org.example.nasaosam.lost.entity.LostReport;
import org.example.nasaosam.lost.entity.LostReportItem;
import org.example.nasaosam.lost.entity.LostReportStatus;
import org.example.nasaosam.lost.repo.LostReportRepository;
import org.example.nasaosam.user.entity.User;
import org.example.nasaosam.user.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
/*
* Znaci ovo je servis za izgubljene oglase
*
* Kontroler prima HTTP zahteve i prosledjuje ga u ovaj servis. Servis sadrzi svu poslovnu logiku jelte:
* provera pravila, pravi entitete, cuva ih i vraca DTO. Kontroler na zna NISTA o bazi, a repo ne zna nista o pravilima
*
* Servis ne vraca entitet kontroloru, uvek vraca DTO (LostReportResponse) jer entitet ima LAZY LOADING za transakcije,
* entiett ima polja koja ne smeju nikako napolje (secret), promena enetieta ne sme slucajno da promeni API koji koeristi FE.
* Ne uzima ID iz tela zahteva. ID uvek dolazi iz JWT
*
* */
@Service
public class LostReportService {
    private static final long EXPIRATION_DAYS = 90;
    private static final long MAX_PAST_DAYS = 365;
    private static final long FUTURE_TOLERANCE_MINUTES = 5;
    private static final int MIN_DESCRIPTION_LENGTH = 40;

    private final LostReportRepository lostReportRepository;
    private final UserRepository userRepository;
    private final HmacService hmacService;
    private final CompletenessScoreCalculator completenessScoreCalculator;

    public LostReportService(LostReportRepository lostReportRepository,
                             UserRepository userRepository,
                             HmacService hmacService,
                             CompletenessScoreCalculator completenessScoreCalculator) {
        this.lostReportRepository = lostReportRepository;
        this.userRepository = userRepository;
        this.hmacService = hmacService;
        this.completenessScoreCalculator = completenessScoreCalculator;
    }

    /*
    * Znaci ovo je metoda koja pravi nov izgubljen oglas sa jednim ili vise predmetima
    *           - proverava zahtev, pravi oglas i predemet, hesira imena, racuna ocenu kompletnosti
    *           - cuva sve u bazu i vraca sacuvan oglas kao DTO
    * Redosled :
    *       1. Sve provere idu pre upisa
    *       2. Vlasnik se ucitava po ID-u tokena
    *       3. Pravi se entitet oglasa i dodaju predmeti
    *       4. Racuna se ocena kompletnosti ()
    *       5. Cuvanje i pretvaranje u DTO
    *
    * Transactional zato sto sve sto se desi ovde mora da bude jedna celina
    *
    * aj boze pomozi
    * */
    @Transactional
    public LostReportResponse create(Long ownerId, CreateLostReportRequest req){
        Instant now;
        Optional<User> foundOwner;
        LostReport report;
        LostReport savedReport;
        List<LostReportItemRequest> itemRequests;

        now = Instant.now();

        //  ---------------- PROVERE ---------------
        validateTimeRange(req.lostFrom(), req.lostTo(), now);
        validateCoordinates(req);
        itemRequests = req.items();
        for(int index = 0; index < itemRequests.size(); index ++){
            validateItem(itemRequests.get(index), index +1);
        }

        foundOwner = userRepository.findById(ownerId);
        if(foundOwner.isEmpty()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, " KORISNIK NE POSTOJI! ");
        }

        // 3--------- PRAVLJENJE OGLASA
        report = new LostReport(
                foundOwner.get(),
                req.lostFrom(),
                req.lostTo(),
                req.city().trim(),
                now.plus(EXPIRATION_DAYS, ChronoUnit.DAYS)
        );

        report.setPlaceDescription(trimToNull(req.placeDescription()));
        report.setLatitude(req.latitude());
        report.setLongitude(req.longitude());
        report.setLocationRadiusMeters(req.locationRadiusMeters());
        report.setPrivateDetails(trimToNull(req.privateDetails()));

        for(int index = 0;index < itemRequests.size(); index++){
            report.addItem(buildItem(itemRequests.get(index))); // OVDE TI SE PRAVI CEO ITEM U OBA SMERA
        }

        //OCENA KOMPLETNOSTI IZ COMPLEENESS SKORE SERVISA
        report.setCompletenessScore(completenessScoreCalculator.calculate(report));

        savedReport = lostReportRepository.save(report);

        return toResponse(savedReport);
    }

    /*
    * Vraca sve oglase prijavljenog korisnika od najnovig ka najstarijem
    *NAPOMENA ZA BUDUcNOST
     *   Za svaki oglas se predmeti uciitavaju posebnim upitom (LAZY). Za nekoliko oglasa
     *   po korisniku nije prob ako lista poraste, resava se jednim upitom sa
     *   JOIN FETCH
    * */
    @Transactional(readOnly = true)
    public List<LostReportResponse> listMine(Long ownerId){
        List<LostReport> reports;
        List<LostReportResponse> responses;

        reports = lostReportRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);

        responses = new ArrayList<>();
        for(int index =0; index < reports.size(); index ++){
            responses.add(toResponse(reports.get(index)));
        }
        return responses;
    }

    // VRACA SAMO JEDAN OGLAS OD KORINSIKA
    @Transactional(readOnly = true)
    public LostReportResponse getMine(Long ownerId, Long reportId){
        return toResponse(findOwnedReport(ownerId, reportId));
    }

    /*
    * FUNKCIJA PROVERAVA VALIDNOST VREMENA DA NE BUDE CUDNO  NES
    * */
    private void validateTimeRange(Instant lostFrom, Instant lostTo, Instant now) {
        if (lostFrom.isAfter(lostTo)) {
            throw badRequest("Početak vremena gubitka ne može biti posle kraja");
        }
        if (lostTo.isAfter(now.plus(FUTURE_TOLERANCE_MINUTES, ChronoUnit.MINUTES))) {
            throw badRequest("Vreme gubitka ne može biti u budućnosti");
        }
        if (lostFrom.isBefore(now.minus(MAX_PAST_DAYS, ChronoUnit.DAYS))) {
            throw badRequest("Gubitak ne može biti stariji od godinu dana");
        }
    }
    private void validateCoordinates(CreateLostReportRequest request) {
        boolean hasLatitude;
        boolean hasLongitude;

        hasLatitude = request.latitude() != null;
        hasLongitude = request.longitude() != null;

        if (hasLatitude != hasLongitude) {
            throw badRequest("Geografska širina i dužina moraju biti zadate zajedno");
        }
        if (request.locationRadiusMeters() != null && !hasLatitude) {
            throw badRequest("Radijus ima smisla samo uz tačku na mapi");
        }
    }
    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }


    /*
    * Proverava jedan premdet po pravilima po vrsti predmeta
    *
    *Pravila po redu:
     *   1. OTHER trazi naziv (customCategory); ostale vrste ga ne smeju imati,
     *      da ne nastane zbrka tipa vrsta "telefon" sa nazivom "kisobran".
     *   2. DOCUMENT trazi vrstu dokumenta; ostali predmeti je ne smeju imati.
     *   3. Opis od najmanje 40 znakova je obavezan za sve osim dokumenata.
     *      Dokument ) izgleda kao svaki drugi tamo opis ne pomaze spajanju,
     *      a kod novcanika, kljuceva i telefona je opis signal za AI debila
     *   4. Hesevi imena su dozvoljeni samo za dokumenta.
     *   5. Hesh sa datumom rodjeneja ne moze bez hesha imena (greska na FE).
     *   6. Platna kartica nema datum rodjenja, pa ni hesh sa datumom.
     *
    * */
    private void validateItem(LostReportItemRequest item, int itemNumber) {
        String prefix;
        boolean isDocument;
        boolean isOther;
        String customCategory;
        String description;

        prefix = "Predmet " + itemNumber + ": ";
        isDocument = item.category() == ItemCategory.DOCUMENT;
        isOther = item.category() == ItemCategory.OTHER;

        // da "   " ne prodje kao popunjeno polje
        customCategory = trimToNull(item.customCategory());
        description = trimToNull(item.description());

        // Pravilo 1: naziv samo za "Ostalo"
        if (isOther && customCategory == null) {
            throw badRequest(prefix + "za vrstu \"Ostalo\" upiši šta je predmet");
        }
        if (!isOther && customCategory != null) {
            throw badRequest(prefix + "naziv predmeta se upisuje samo za vrstu \"Ostalo\"");
        }

        // Pravilo 2: vrsta dokumenta samo za dokumenta
        if (isDocument && item.documentType() == null) {
            throw badRequest(prefix + "izaberi vrstu dokumenta");
        }
        if (!isDocument && item.documentType() != null) {
            throw badRequest(prefix + "vrsta dokumenta se bira samo za dokumenta");
        }

        // Pravilo 3: obavezan detaljan opis za sve osim dokumenata.
        // Poruka kaze i STA da opise, da vlasnik ne bi samo dopunio tekst tackama
        if (!isDocument) {
            if (description == null || description.length() < MIN_DESCRIPTION_LENGTH) {
                throw badRequest(prefix + "opis mora imati najmanje " + MIN_DESCRIPTION_LENGTH
                        + " znakova. Opiši izgled, oštećenja i sve po čemu se predmet razlikuje.");
            }
        }

        // Pravilo 4: ime samo za dokumenta.
        // Za predmete koji nisu dokumenta posle ove provere nema vise sta da se proverava,
        // pa se izlazi iz metode (return), a ostatak vazi smo za dokumenta.
        if (!isDocument) {
            if (item.nameHash() != null || item.nameDobHash() != null) {
                throw badRequest(prefix + "ime se unosi samo za dokumenta");
            }
            return;
        }

        // Pravilo 5: frontend uvek pravi oba hesha iz istog imena; ako postoji samo drugi, nesto je sjebano
        if (item.nameDobHash() != null && item.nameHash() == null) {
            throw badRequest(prefix + "nedostaje heš imena");
        }

        // Pravilo 6: na platnoj kartici nema datuma rodjenjea
        if (item.documentType() == DocumentType.BANK_CARD && item.nameDobHash() != null) {
            throw badRequest(prefix + "platna kartica nema datum rođenja");
        }
    }

    //  ---------------------------------------- HELPERI ------------------------------------------------------------
    //  ---------------------------------------- HELPERI ------------------------------------------------------------
    //  ---------------------------------------- HELPERI ------------------------------------------------------------
    //  ---------------------------------------- HELPERI ------------------------------------------------------------

    /*
    *
    * pRAVI entitet predmeta od podataka iz DTO zahteva
    *
    * HESEVI IMENA
     *   Sa telefona stizew SHA-256 hesh imena (pravo ime nikad ne napusta fon).
     *   HmacService preko njega primenjuje HMAC sa tajnim kljucem sa servera i samo
     *   taj rezultat ide u bazu. HmacService odbija sve sto nije ispravan SHA-256
     *    pa se pravo ime nikad ne sacuva.
    * */
    private LostReportItem buildItem(LostReportItemRequest req){
        LostReportItem item;
        item = new LostReportItem(req.category());

        item.setCustomCategory(trimToNull(req.customCategory()));
        item.setDocumentType(req.documentType());
        item.setColor(req.color());
        item.setBrand(trimToNull(req.brand()));
        item.setMaterial(trimToNull(req.material()));
        item.setDescription(trimToNull(req.description()));
        item.setDistinctiveMarks(trimToNull(req.distinctiveMarks()));

        if (req.nameHash() != null) {
            item.setNameHash(hmacService.hashClientHash(req.nameHash()));
        }
        if (req.nameDobHash() != null) {
            item.setNameDobHash(hmacService.hashClientHash(req.nameDobHash()));
        }
        return item;
    }

    /*UCITAVA OGLAS SAMO AKO PRIPADA DATOM VLASKINU SA ID*/
    private LostReport findOwnedReport(Long ownerId, Long reportId){
        Optional<LostReport> foundReport;
        foundReport = lostReportRepository.findByIdAndOwnerId(reportId, ownerId);
        if(foundReport.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OGlas nije pronadjen");
        }
        return foundReport.get();
    }

    //PRETVARA ENEITTET OGLASA U DTO KOJI IDE NA FEEEEE SINE
    private LostReportResponse toResponse(LostReport report){
        List<LostReportItemResponse> itemResponses;
        List<LostReportItem> items;
        itemResponses = new ArrayList<>();
        items = report.getItems();
        for(int i=0;i< items.size(); i++){
            itemResponses.add(toItemResponse(items.get(i)));
        }

        return new LostReportResponse(
                report.getId(),
                report.getStatus(),
                report.getLostFrom(),
                report.getLostTo(),
                report.getCity(),
                report.getPlaceDescription(),
                report.getLatitude(),
                report.getLongitude(),
                report.getLocationRadiusMeters(),
                report.getPrivateDetails(),
                report.getCompletenessScore(),
                report.getExpiresAt(),
                report.getClosedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                itemResponses
        );
    }
    // PRETVARA ENTITET PREDMETA U DTO KOJI KORISTI ENTITET OGLASA
    private LostReportItemResponse toItemResponse(LostReportItem item){
        return new LostReportItemResponse(
                item.getId(),
                item.getCategory(),
                item.getCustomCategory(),
                item.getDocumentType(),
                item.getColor(),
                item.getBrand(),
                item.getMaterial(),
                item.getDescription(),
                item.getDistinctiveMarks(),
                item.getNameHash() != null,
                item.getNameDobHash() != null
        );
    }


    private String trimToNull(String value) {
        String trimmed;

        if (value == null) {
            return null;
        }
        trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

}
