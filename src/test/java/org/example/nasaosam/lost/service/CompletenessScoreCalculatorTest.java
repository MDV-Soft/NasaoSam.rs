package org.example.nasaosam.lost.service;

import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;
import org.example.nasaosam.lost.entity.LostReport;
import org.example.nasaosam.lost.entity.LostReportItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
// -------------------- CLAUDE PISAO ------------------

class CompletenessScoreCalculatorTest {

    private final CompletenessScoreCalculator calculator = new CompletenessScoreCalculator();

    // Oglas bez vlasnika je u redu za test, jer se ništa ne čuva u bazi
    private LostReport emptyReport() {
        Instant now;

        now = Instant.now();
        return new LostReport(null, now, now, "Niš", now);
    }

    private LostReportItem fullWallet() {
        LostReportItem wallet;

        wallet = new LostReportItem(ItemCategory.WALLET);
        wallet.setColor(ItemColor.BLACK);
        wallet.setBrand("Tommy Hilfiger");
        wallet.setMaterial("koža");
        wallet.setDistinctiveMarks("izlizan donji levi ugao");
        wallet.setDescription("a".repeat(200));
        return wallet;
    }

    @Test
    void minimalWalletGetsLowScore() {
        LostReport report;
        LostReportItem wallet;

        report = emptyReport();
        wallet = new LostReportItem(ItemCategory.WALLET);
        wallet.setDescription("a".repeat(40));
        report.addItem(wallet);

        // Predmet: 20 (opis od 40 znakova) -> 20 * 0.7 = 14; oglas: 0
        assertEquals(14, calculator.calculate(report));
    }

    @Test
    void fullReportGetsMaximumScore() {
        LostReport report;

        report = emptyReport();
        report.setPlaceDescription("autobus 34, smer Bubanj");
        report.setLatitude(new BigDecimal("43.320902"));
        report.setLongitude(new BigDecimal("21.895759"));
        report.setPrivateDetails("slika ćerke");
        report.addItem(fullWallet());

        // Oglas: 30; predmet: 100 -> 70; ukupno 100
        assertEquals(100, calculator.calculate(report));
    }

    @Test
    void scoreIsAverageOfItems() {
        LostReport report;
        LostReportItem idCard;

        report = emptyReport();
        report.setPlaceDescription("autobus 34");
        report.setLatitude(new BigDecimal("43.320902"));
        report.setLongitude(new BigDecimal("21.895759"));
        report.setPrivateDetails("slika ćerke");
        report.addItem(fullWallet());

        idCard = new LostReportItem(ItemCategory.DOCUMENT);
        idCard.setDocumentType(DocumentType.ID_CARD);
        idCard.setNameHash("x".repeat(64));
        report.addItem(idCard);

        // Novčanik 100, lična karta 30 + 40 = 70 -> prosek 85 -> 85 * 0.7 = 59.5 -> 60; plus oglas 30 = 90
        assertEquals(90, calculator.calculate(report));
    }

    @Test
    void documentWithBothHashesGetsFullItemScore() {
        LostReportItem idCard;

        idCard = new LostReportItem(ItemCategory.DOCUMENT);
        idCard.setDocumentType(DocumentType.ID_CARD);
        idCard.setNameHash("x".repeat(64));
        idCard.setNameDobHash("y".repeat(64));

        assertEquals(100, calculator.calculateItemScore(idCard));
    }

    @Test
    void descriptionLengthChangesScore() {
        LostReportItem keys;

        keys = new LostReportItem(ItemCategory.KEYS);

        keys.setDescription("a".repeat(39));
        assertEquals(0, calculator.calculateItemScore(keys));

        keys.setDescription("a".repeat(40));
        assertEquals(20, calculator.calculateItemScore(keys));

        keys.setDescription("a".repeat(100));
        assertEquals(30, calculator.calculateItemScore(keys));

        keys.setDescription("a".repeat(200));
        assertEquals(40, calculator.calculateItemScore(keys));
    }
}