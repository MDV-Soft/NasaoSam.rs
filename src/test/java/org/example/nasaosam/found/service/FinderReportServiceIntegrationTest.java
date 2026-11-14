package org.example.nasaosam.found.service;

import jakarta.persistence.EntityManager;
import org.example.nasaosam.auth.service.OneTimeTokenService;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;
import org.example.nasaosam.found.dto.CreateFoundReportRequest;
import org.example.nasaosam.found.dto.FinderReportResponse;
import org.example.nasaosam.found.dto.FoundReportItemRequest;
import org.example.nasaosam.found.dto.UpdateItemLocationRequest;
import org.example.nasaosam.found.entity.FoundReport;
import org.example.nasaosam.found.entity.FoundReportStatus;
import org.example.nasaosam.found.entity.ItemLocation;
import org.example.nasaosam.found.repo.FoundReportRepository;
import org.example.nasaosam.notification.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Rute nalazača na nivou servisa, sa pravom bazom (MySQL na portu 3307 mora da radi).
 * Aktivan nalaz se pravi pravim putem: prijava, pa otvaranje linka.
 */
@SpringBootTest
@Transactional
class FinderReportServiceIntegrationTest {

    private static final String LINK_BASE = "http://localhost:4200/nalaz?token=";

    @Autowired
    private FinderReportService finderReportService;

    @Autowired
    private FinderAccessService finderAccessService;

    @Autowired
    private FoundReportService foundReportService;

    @Autowired
    private FoundReportRepository foundReportRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private EmailService emailService;

    // ---------------------------------------------------------------
    // Pomoćne metode
    // ---------------------------------------------------------------

    private CreateFoundReportRequest validRequest(String email, String city) {
        FoundReportItemRequest wallet;

        wallet = new FoundReportItemRequest(ItemCategory.WALLET, null, null, ItemColor.BLACK,
                null, "koža", null, null, null, null);
        return new CreateFoundReportRequest(
                email, "064 123 4567",
                Instant.now().minus(1, ChronoUnit.HOURS),
                city, "autobus 34, kod Bubnja",
                new BigDecimal("43.320902"), new BigDecimal("21.895759"),
                ItemLocation.WITH_FINDER, null,
                "Šta je bilo u novčaniku osim dokumenata?", "slika ćerke i kartica teretane",
                List.of(wallet));
    }

    // Prijava + otvaranje linka: vraća ID aktivnog nalaza (isto što bi bilo u tokenu nalazača)
    private Long createActiveReport(String email, String city) {
        ArgumentCaptor<String> linkCaptor;
        String rawToken;
        Long reportId;

        foundReportService.create(validRequest(email, city));
        linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendFinderAccessLink(anyString(), linkCaptor.capture());
        rawToken = linkCaptor.getValue().substring(LINK_BASE.length());

        entityManager.flush();
        reportId = jdbcTemplate.queryForObject(
                "SELECT found_report_id FROM one_time_tokens WHERE token_hash = ?", Long.class,
                OneTimeTokenService.hash(rawToken));
        entityManager.clear();

        finderAccessService.openLink(rawToken);
        return reportId;
    }

    private void assertStatus(int expectedStatus, Runnable action) {
        ResponseStatusException exception;

        exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expectedStatus, exception.getStatusCode().value());
    }

    // ---------------------------------------------------------------
    // Pregled
    // ---------------------------------------------------------------

    @Test
    void returnsOwnReportWithItems() {
        Long reportId;
        FinderReportResponse response;

        reportId = createActiveReport("nalazac@test.rs", "Niš");

        response = finderReportService.getReport(reportId);

        assertEquals(FoundReportStatus.ACTIVE, response.status());
        assertEquals("Niš", response.city());
        assertEquals(ItemLocation.WITH_FINDER, response.itemLocation());
        assertEquals("Šta je bilo u novčaniku osim dokumenata?", response.controlQuestion());
        assertNotNull(response.activatedAt());
        assertEquals(1, response.items().size());
        assertEquals(ItemCategory.WALLET, response.items().get(0).category());
    }

    // Zaštita od buduće greške: ako neko doda polje sa kontaktom ili odgovorom, test pada
    @Test
    void responseHasNoContactOrControlAnswer() {
        RecordComponent[] components;
        String name;

        components = FinderReportResponse.class.getRecordComponents();
        for (int index = 0; index < components.length; index++) {
            name = components[index].getName().toLowerCase();
            assertFalse(name.contains("email"), "Odgovor ne sme imati mejl");
            assertFalse(name.contains("phone"), "Odgovor ne sme imati telefon");
            assertFalse(name.contains("answer"), "Odgovor ne sme imati kontrolni odgovor");
            assertFalse(name.equals("id"), "Odgovor ne sme imati ID nalaza");
        }
    }

    @Test
    void eachIdGivesOnlyItsOwnReport() {
        Long firstId;
        Long secondId;

        firstId = createActiveReport("prvi@test.rs", "Niš");
        secondId = createActiveReport("drugi@test.rs", "Beograd");

        // Servis radi sa tačno onim ID-jem koji dobije (u praksi iz tokena)
        assertEquals("Niš", finderReportService.getReport(firstId).city());
        assertEquals("Beograd", finderReportService.getReport(secondId).city());
    }

    // ---------------------------------------------------------------
    // Promena gde je predmet
    // ---------------------------------------------------------------

    @Test
    void movesItemToInstitutionWithHandoverInfo() {
        Long reportId;
        FinderReportResponse response;

        reportId = createActiveReport("nalazac@test.rs", "Niš");

        response = finderReportService.updateItemLocation(reportId,
                new UpdateItemLocationRequest(ItemLocation.AT_INSTITUTION, "  Autobuska stanica, šalter 3, 8-20h  "));

        assertEquals(ItemLocation.AT_INSTITUTION, response.itemLocation());
        assertEquals("Autobuska stanica, šalter 3, 8-20h", response.handoverInfo());
    }

    @Test
    void movingBackToFinderClearsHandoverInfo() {
        Long reportId;
        FinderReportResponse response;

        reportId = createActiveReport("nalazac@test.rs", "Niš");
        finderReportService.updateItemLocation(reportId,
                new UpdateItemLocationRequest(ItemLocation.AT_POLICE, "Policijska stanica Niš"));

        response = finderReportService.updateItemLocation(reportId,
                new UpdateItemLocationRequest(ItemLocation.WITH_FINDER, null));

        assertEquals(ItemLocation.WITH_FINDER, response.itemLocation());
        assertNull(response.handoverInfo());
    }

    @Test
    void locationAwayFromFinderNeedsHandoverInfo() {
        Long reportId;

        reportId = createActiveReport("nalazac@test.rs", "Niš");

        assertStatus(400, () -> finderReportService.updateItemLocation(reportId,
                new UpdateItemLocationRequest(ItemLocation.AT_POLICE, "   ")));
        assertStatus(400, () -> finderReportService.updateItemLocation(reportId,
                new UpdateItemLocationRequest(ItemLocation.AT_INSTITUTION, null)));
    }

    @Test
    void locationWithFinderRejectsHandoverInfo() {
        Long reportId;

        reportId = createActiveReport("nalazac@test.rs", "Niš");

        assertStatus(400, () -> finderReportService.updateItemLocation(reportId,
                new UpdateItemLocationRequest(ItemLocation.WITH_FINDER, "Neka adresa")));
    }

    // ---------------------------------------------------------------
    // Povlačenje
    // ---------------------------------------------------------------

    @Test
    void withdrawClosesReport() {
        Long reportId;
        FinderReportResponse response;
        FoundReport report;

        reportId = createActiveReport("nalazac@test.rs", "Niš");

        response = finderReportService.withdraw(reportId);

        assertEquals(FoundReportStatus.WITHDRAWN, response.status());
        report = foundReportRepository.findById(reportId).orElseThrow();
        assertEquals(FoundReportStatus.WITHDRAWN, report.getStatus());
        assertNotNull(report.getClosedAt());
    }

    @Test
    void withdrawnReportCanBeViewedButNotChanged() {
        Long reportId;

        reportId = createActiveReport("nalazac@test.rs", "Niš");
        finderReportService.withdraw(reportId);

        assertEquals(FoundReportStatus.WITHDRAWN, finderReportService.getReport(reportId).status());
        assertStatus(409, () -> finderReportService.withdraw(reportId));
        assertStatus(409, () -> finderReportService.updateItemLocation(reportId,
                new UpdateItemLocationRequest(ItemLocation.AT_POLICE, "Policijska stanica Niš")));
    }

    @Test
    void unknownReportGivesNotFound() {
        assertStatus(404, () -> finderReportService.getReport(999_999_999L));
        assertStatus(404, () -> finderReportService.withdraw(999_999_999L));
    }
}