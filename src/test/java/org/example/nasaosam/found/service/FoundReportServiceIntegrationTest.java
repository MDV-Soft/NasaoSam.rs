package org.example.nasaosam.found.service;

import jakarta.persistence.EntityManager;
import org.example.nasaosam.auth.service.OneTimeTokenService;
import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;
import org.example.nasaosam.found.dto.CreateFoundReportRequest;
import org.example.nasaosam.found.dto.FoundReportItemRequest;
import org.example.nasaosam.found.dto.FoundReportSubmittedResponse;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Prijava nalaza sa pravom bazom (MySQL na portu 3307 mora da radi), bez kontrolera.
 * Slanje mejla je zamenjeno probnim objektom, pa se link hvata i proverava umesto da se ispisuje.
 * @Transactional: posle svakog testa sve se poništava.
 */
@SpringBootTest
@Transactional
class FoundReportServiceIntegrationTest {

    private static final String LINK_BASE = "http://localhost:4200/nalaz?token=";
    private static final String CLIENT_NAME_HASH = "ab".repeat(32);
    private static final String CLIENT_NAME_DOB_HASH = "cd".repeat(32);

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

    private FoundReportItemRequest walletItem() {
        return new FoundReportItemRequest(ItemCategory.WALLET, null, null, ItemColor.BLACK,
                null, "koža", null, null, null, null);
    }

    private FoundReportItemRequest idCardItem(String nameHash, String nameDobHash) {
        return new FoundReportItemRequest(ItemCategory.DOCUMENT, null, DocumentType.ID_CARD, null,
                null, null, null, null, nameHash, nameDobHash);
    }

    private CreateFoundReportRequest request(String email, String phone, ItemLocation location,
                                             String handoverInfo, List<FoundReportItemRequest> items) {
        return new CreateFoundReportRequest(
                email, phone,
                Instant.now().minus(1, ChronoUnit.HOURS),
                "Niš", "autobus 34, kod Bubnja",
                new BigDecimal("43.320902"), new BigDecimal("21.895759"),
                location, handoverInfo,
                "Šta je bilo u novčaniku osim dokumenata?", "slika ćerke i kartica teretane",
                items);
    }

    private CreateFoundReportRequest validRequest() {
        return request("nalazac@test.rs", "064 123 4567", ItemLocation.WITH_FINDER, null,
                List.of(walletItem(), idCardItem(CLIENT_NAME_HASH, CLIENT_NAME_DOB_HASH)));
    }

    // Link koji je "poslat" nalazaču (uhvaćen umesto slanja pravog mejla)
    private String capturedLink() {
        ArgumentCaptor<String> linkCaptor;

        linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendFinderAccessLink(anyString(), linkCaptor.capture());
        return linkCaptor.getValue();
    }

    private String tokenFrom(String link) {
        return link.substring(LINK_BASE.length());
    }

    // Odgovor ne sadrži ID nalaza, pa se nalaz traži preko heša tokena iz linka
    private FoundReport loadReportByLink(String link) {
        Long reportId;

        // Prvo upis u bazu, pa tek onda čitanje preko JDBC-a
        entityManager.flush();
        reportId = jdbcTemplate.queryForObject(
                "SELECT found_report_id FROM one_time_tokens WHERE token_hash = ?", Long.class,
                OneTimeTokenService.hash(tokenFrom(link)));
        entityManager.clear();
        return foundReportRepository.findById(reportId).orElseThrow();
    }

    private int statusOf(ResponseStatusException exception) {
        return exception.getStatusCode().value();
    }

    private void assertRejected(CreateFoundReportRequest badRequest) {
        ResponseStatusException exception;

        exception = assertThrows(ResponseStatusException.class, () -> foundReportService.create(badRequest));
        assertEquals(400, statusOf(exception));
        // Odbijena prijava ne sme da pošalje link
        verifyNoInteractions(emailService);
    }

    // ---------------------------------------------------------------
    // Uspešna prijava
    // ---------------------------------------------------------------

    @Test
    void savesPendingReportWithNormalizedContact() {
        FoundReport report;
        Instant expectedExpiry;

        foundReportService.create(request("  Nalazac@Test.RS ", "064 123 4567", ItemLocation.WITH_FINDER, null,
                List.of(walletItem(), idCardItem(CLIENT_NAME_HASH, CLIENT_NAME_DOB_HASH))));
        report = loadReportByLink(capturedLink());
        expectedExpiry = Instant.now().plus(24, ChronoUnit.HOURS);

        assertEquals(FoundReportStatus.PENDING_EMAIL, report.getStatus());
        assertEquals("nalazac@test.rs", report.getFinderEmail());
        assertEquals("+381641234567", report.getFinderPhone());
        assertTrue(report.isShareEmail());
        assertTrue(report.isSharePhone());
        assertEquals(2, report.getItems().size());
        assertNull(report.getBlurredPhotoKey());
        // Rok je oko 24 sata od sada (dozvoljena razlika od minut zbog trajanja testa)
        assertTrue(Duration.between(report.getExpiresAt(), expectedExpiry).abs().toMinutes() < 2);
    }

    @Test
    void phoneIsOptionalAndThenOnlyEmailIsShared() {
        FoundReport report;

        foundReportService.create(request("nalazac@test.rs", null, ItemLocation.WITH_FINDER, null,
                List.of(walletItem())));
        report = loadReportByLink(capturedLink());

        assertNull(report.getFinderPhone());
        assertTrue(report.isShareEmail());
        assertFalse(report.isSharePhone());
    }

    @Test
    void blankPhoneIsTreatedAsMissing() {
        FoundReport report;

        foundReportService.create(request("nalazac@test.rs", "   ", ItemLocation.WITH_FINDER, null,
                List.of(walletItem())));
        report = loadReportByLink(capturedLink());

        assertNull(report.getFinderPhone());
        assertFalse(report.isSharePhone());
    }

    @Test
    void itemWithoutDescriptionIsAccepted() {
        // Opis je neobavezan: nalazač potvrđuje ono što je AI prepoznao, ne piše tekst
        foundReportService.create(request("nalazac@test.rs", null, ItemLocation.WITH_FINDER, null,
                List.of(walletItem())));

        verify(emailService).sendFinderAccessLink(anyString(), anyString());
    }

    @Test
    void itemAtPoliceWithAddressIsAccepted() {
        FoundReport report;

        foundReportService.create(request("nalazac@test.rs", null, ItemLocation.AT_POLICE,
                "PS Niš, Nikole Pašića 1, šalter 3", List.of(idCardItem(CLIENT_NAME_HASH, null))));
        report = loadReportByLink(capturedLink());

        assertEquals(ItemLocation.AT_POLICE, report.getItemLocation());
        assertEquals("PS Niš, Nikole Pašića 1, šalter 3", report.getHandoverInfo());
    }

    // ---------------------------------------------------------------
    // Šifrovanje, heševi i token
    // ---------------------------------------------------------------

    @Test
    void contactAndAnswerAreEncryptedInDatabase() {
        String link;
        Long reportId;
        String emailInDatabase;
        String phoneInDatabase;
        String answerInDatabase;

        foundReportService.create(validRequest());
        link = capturedLink();
        entityManager.flush();
        reportId = jdbcTemplate.queryForObject(
                "SELECT found_report_id FROM one_time_tokens WHERE token_hash = ?", Long.class,
                OneTimeTokenService.hash(tokenFrom(link)));

        emailInDatabase = jdbcTemplate.queryForObject(
                "SELECT finder_email FROM found_reports WHERE id = ?", String.class, reportId);
        phoneInDatabase = jdbcTemplate.queryForObject(
                "SELECT finder_phone FROM found_reports WHERE id = ?", String.class, reportId);
        answerInDatabase = jdbcTemplate.queryForObject(
                "SELECT control_answer FROM found_reports WHERE id = ?", String.class, reportId);

        assertTrue(emailInDatabase.startsWith("v1:"));
        assertFalse(emailInDatabase.contains("nalazac"));
        assertTrue(phoneInDatabase.startsWith("v1:"));
        assertFalse(phoneInDatabase.contains("641234567"));
        assertTrue(answerInDatabase.startsWith("v1:"));
        assertFalse(answerInDatabase.contains("ćerke"));
    }

    @Test
    void nameHashesAreStoredAsHmacNotClientHash() {
        FoundReport report;
        String storedNameHash;

        foundReportService.create(validRequest());
        report = loadReportByLink(capturedLink());

        storedNameHash = report.getItems().stream()
                .map(item -> item.getNameHash())
                .filter(hash -> hash != null)
                .findFirst().orElseThrow();

        assertEquals(64, storedNameHash.length());
        assertNotEquals(CLIENT_NAME_HASH, storedNameHash);
    }

    @Test
    void linkContainsTokenWhoseHashIsStoredNotTheToken() {
        String link;
        String token;
        String storedHash;
        String purpose;
        Instant tokenExpiry;

        foundReportService.create(validRequest());
        entityManager.flush();
        link = capturedLink();
        token = tokenFrom(link);

        assertTrue(link.startsWith(LINK_BASE));
        // 43 znaka od slova, cifara, "-" i "_" (siguran u adresi bez kodiranja)
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"), token);

        storedHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM one_time_tokens WHERE token_hash = ?", String.class,
                OneTimeTokenService.hash(token));
        purpose = jdbcTemplate.queryForObject(
                "SELECT purpose FROM one_time_tokens WHERE token_hash = ?", String.class, storedHash);

        assertEquals(OneTimeTokenService.hash(token), storedHash);
        assertNotEquals(token, storedHash);
        assertEquals("FINDER_ACCESS", purpose);

        // Izvorni token ne sme da postoji nigde u bazi
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM one_time_tokens WHERE token_hash = ?", Integer.class, token));
    }

    @Test
    void everySubmissionGetsDifferentToken() {
        ArgumentCaptor<String> linkCaptor;

        foundReportService.create(validRequest());
        foundReportService.create(validRequest());

        linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(2)).sendFinderAccessLink(anyString(), linkCaptor.capture());

        assertNotEquals(linkCaptor.getAllValues().get(0), linkCaptor.getAllValues().get(1));
    }

    @Test
    void responseRevealsNeitherContactNorToken() {
        FoundReportSubmittedResponse response;
        String token;

        response = foundReportService.create(validRequest());
        token = tokenFrom(capturedLink());

        assertNotNull(response.linkExpiresAt());
        assertFalse(response.toString().contains("nalazac"));
        assertFalse(response.toString().contains("641234567"));
        assertFalse(response.toString().contains(token));
    }

    @Test
    void emailIsSentToTheNormalizedAddress() {
        foundReportService.create(request("  Nalazac@Test.RS ", null, ItemLocation.WITH_FINDER, null,
                List.of(walletItem())));

        verify(emailService).sendFinderAccessLink(eq("nalazac@test.rs"), anyString());
    }

    // ---------------------------------------------------------------
    // Pravila (sva moraju da vrate 400 i da ne pošalju link)
    // ---------------------------------------------------------------

    @Test
    void reportWithoutItemsIsRejected() {
        assertRejected(request("nalazac@test.rs", null, ItemLocation.WITH_FINDER, null, List.of()));
    }

    @Test
    void otherCategoryRequiresCustomName() {
        FoundReportItemRequest umbrella;

        umbrella = new FoundReportItemRequest(ItemCategory.OTHER, null, null, ItemColor.BLACK,
                null, null, null, null, null, null);
        assertRejected(request("nalazac@test.rs", null, ItemLocation.WITH_FINDER, null, List.of(umbrella)));
    }

    @Test
    void nameHashOnWalletIsRejected() {
        FoundReportItemRequest walletWithName;

        walletWithName = new FoundReportItemRequest(ItemCategory.WALLET, null, null, null,
                null, null, null, null, CLIENT_NAME_HASH, null);
        assertRejected(request("nalazac@test.rs", null, ItemLocation.WITH_FINDER, null, List.of(walletWithName)));
    }

    @Test
    void bankCardWithBirthDateIsRejected() {
        FoundReportItemRequest bankCard;

        bankCard = new FoundReportItemRequest(ItemCategory.DOCUMENT, null, DocumentType.BANK_CARD, null,
                null, null, null, null, CLIENT_NAME_HASH, CLIENT_NAME_DOB_HASH);
        assertRejected(request("nalazac@test.rs", null, ItemLocation.WITH_FINDER, null, List.of(bankCard)));
    }

    @Test
    void documentWithoutTypeIsRejected() {
        FoundReportItemRequest document;

        document = new FoundReportItemRequest(ItemCategory.DOCUMENT, null, null, null,
                null, null, null, null, null, null);
        assertRejected(request("nalazac@test.rs", null, ItemLocation.WITH_FINDER, null, List.of(document)));
    }

    @Test
    void itemAtPoliceWithoutAddressIsRejected() {
        assertRejected(request("nalazac@test.rs", null, ItemLocation.AT_POLICE, null, List.of(walletItem())));
    }

    @Test
    void itemWithFinderMustNotHaveAddress() {
        assertRejected(request("nalazac@test.rs", null, ItemLocation.WITH_FINDER, "Neka adresa", List.of(walletItem())));
    }

    @Test
    void futureFoundTimeIsRejected() {
        CreateFoundReportRequest future;

        future = new CreateFoundReportRequest("nalazac@test.rs", null,
                Instant.now().plus(1, ChronoUnit.HOURS), "Niš", null,
                new BigDecimal("43.3"), new BigDecimal("21.9"), ItemLocation.WITH_FINDER, null,
                "Pitanje?", "Odgovor", List.of(walletItem()));
        assertRejected(future);
    }

    @Test
    void tooOldFoundTimeIsRejected() {
        CreateFoundReportRequest old;

        old = new CreateFoundReportRequest("nalazac@test.rs", null,
                Instant.now().minus(31, ChronoUnit.DAYS), "Niš", null,
                new BigDecimal("43.3"), new BigDecimal("21.9"), ItemLocation.WITH_FINDER, null,
                "Pitanje?", "Odgovor", List.of(walletItem()));
        assertRejected(old);
    }

    @Test
    void missingMapPointIsRejected() {
        CreateFoundReportRequest noPoint;

        noPoint = new CreateFoundReportRequest("nalazac@test.rs", null,
                Instant.now().minus(1, ChronoUnit.HOURS), "Niš", null,
                null, null, ItemLocation.WITH_FINDER, null,
                "Pitanje?", "Odgovor", List.of(walletItem()));
        assertRejected(noPoint);
    }

    @Test
    void invalidPhoneIsRejected() {
        // Fiksni broj nije mobilni
        assertRejected(request("nalazac@test.rs", "011 234 5678", ItemLocation.WITH_FINDER, null,
                List.of(walletItem())));
    }

    @Test
    void realNameInsteadOfHashIsRejected() {
        assertRejected(request("nalazac@test.rs", null, ItemLocation.WITH_FINDER, null,
                List.of(idCardItem("Petar Petrovic", null))));
    }
}