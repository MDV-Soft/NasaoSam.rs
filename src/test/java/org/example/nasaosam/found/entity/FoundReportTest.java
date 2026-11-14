package org.example.nasaosam.found.entity;

import org.example.nasaosam.common.item.ItemCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TEST TEST TEST TEST TEST TEST TEST TESTTEST TEST TEST TESTTEST TEST TEST TEST
// TEST TEST TEST TESTTEST TEST TEST TESTTEST TEST TEST TESTTEST TEST TEST TESTTEST TEST TEST TEST
//TEST TEST TEST TESTTEST TEST TEST TESTTEST TEST TEST TESTTEST TEST TEST TESTTEST TEST TEST TEST
// --------------------------------- CLAUDE PISAO- --------------------------------
class FoundReportTest {

    private FoundReport createReport() {
        return new FoundReport(FoundReportSource.CITIZEN, "nalazac@test.rs", Instant.now(), "Niš",
                new BigDecimal("43.320902"), new BigDecimal("21.895759"), ItemLocation.WITH_FINDER,
                "Šta je u novčaniku?", "slika ćerke", Instant.now().plus(1, ChronoUnit.DAYS));
    }

    @Test
    void newReportWaitsForEmailConfirmation() {
        FoundReport report;

        report = createReport();

        assertEquals(FoundReportStatus.PENDING_EMAIL, report.getStatus());
        assertFalse(report.isActive());
        assertNull(report.getActivatedAt());
        assertNull(report.getClosedAt());
    }

    @Test
    void contactSharingDefaultsMatchTheDatabase() {
        FoundReport report;

        report = createReport();

        assertTrue(report.isShareEmail());
        assertFalse(report.isSharePhone());
    }

    @Test
    void activateMovesToActiveAndSetsNewExpiry() {
        FoundReport report;
        Instant now;
        Instant newExpiry;

        report = createReport();
        now = Instant.now();
        newExpiry = now.plus(60, ChronoUnit.DAYS);

        report.activate(now, newExpiry);

        assertEquals(FoundReportStatus.ACTIVE, report.getStatus());
        assertTrue(report.isActive());
        assertEquals(now, report.getActivatedAt());
        assertEquals(newExpiry, report.getExpiresAt());
    }

    @Test
    void activateTwiceIsRejected() {
        FoundReport report;

        report = createReport();
        report.activate(Instant.now(), Instant.now().plus(60, ChronoUnit.DAYS));

        assertThrows(IllegalStateException.class,
                () -> report.activate(Instant.now(), Instant.now().plus(60, ChronoUnit.DAYS)));
    }

    @Test
    void withdrawWorksForPendingAndActiveReports() {
        FoundReport pendingReport;
        FoundReport activeReport;

        pendingReport = createReport();
        pendingReport.withdraw(Instant.now());
        assertEquals(FoundReportStatus.WITHDRAWN, pendingReport.getStatus());
        assertNotNull(pendingReport.getClosedAt());

        activeReport = createReport();
        activeReport.activate(Instant.now(), Instant.now().plus(60, ChronoUnit.DAYS));
        activeReport.withdraw(Instant.now());
        assertEquals(FoundReportStatus.WITHDRAWN, activeReport.getStatus());
    }

    @Test
    void returnedIsPossibleOnlyForActiveReport() {
        FoundReport report;

        report = createReport();
        // Nepotvrđen nalaz ne može biti vraćen
        assertThrows(IllegalStateException.class, () -> report.markReturned(Instant.now()));

        report.activate(Instant.now(), Instant.now().plus(60, ChronoUnit.DAYS));
        report.markReturned(Instant.now());
        assertEquals(FoundReportStatus.RETURNED, report.getStatus());
    }

    @Test
    void expiryWorksForPendingAndActiveReports() {
        FoundReport pendingReport;
        FoundReport activeReport;

        pendingReport = createReport();
        pendingReport.markExpired(Instant.now());
        assertEquals(FoundReportStatus.EXPIRED, pendingReport.getStatus());

        activeReport = createReport();
        activeReport.activate(Instant.now(), Instant.now().plus(60, ChronoUnit.DAYS));
        activeReport.markExpired(Instant.now());
        assertEquals(FoundReportStatus.EXPIRED, activeReport.getStatus());
    }

    @Test
    void finishedReportCannotChangeAgain() {
        FoundReport report;

        report = createReport();
        report.withdraw(Instant.now());

        assertThrows(IllegalStateException.class, () -> report.withdraw(Instant.now()));
        assertThrows(IllegalStateException.class, () -> report.markExpired(Instant.now()));
        assertThrows(IllegalStateException.class, () -> report.markReturned(Instant.now()));
        assertThrows(IllegalStateException.class,
                () -> report.activate(Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS)));
    }

    @Test
    void addItemLinksBothSides() {
        FoundReport report;
        FoundReportItem item;

        report = createReport();
        item = new FoundReportItem(ItemCategory.WALLET);
        report.addItem(item);

        assertEquals(1, report.getItems().size());
        assertSame(report, item.getFoundReport());

        report.removeItem(item);
        assertEquals(0, report.getItems().size());
        assertNull(item.getFoundReport());
    }
}