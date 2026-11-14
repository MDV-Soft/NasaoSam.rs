package org.example.nasaosam.found.service;

import jakarta.persistence.EntityManager;
import org.example.nasaosam.auth.entity.OneTimeTokenPurpose;
import org.example.nasaosam.auth.service.FinderTokenService;
import org.example.nasaosam.auth.service.OneTimeTokenService;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;
import org.example.nasaosam.found.dto.CreateFoundReportRequest;
import org.example.nasaosam.found.dto.FinderAccessResponse;
import org.example.nasaosam.found.dto.FoundReportItemRequest;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Otvaranje linka sa pravom bazom (MySQL na portu 3307 mora da radi), bez kontrolera.
 * Nalaz se pravi običnom prijavom, a link se hvata umesto slanja pravog mejla.
 */
@SpringBootTest
@Transactional
class FinderAccessServiceIntegrationTest {

    private static final String LINK_BASE = "http://localhost:4200/nalaz?token=";

    @Autowired
    private FinderAccessService finderAccessService;

    @Autowired
    private FoundReportService foundReportService;

    @Autowired
    private FoundReportRepository foundReportRepository;

    @Autowired
    private OneTimeTokenService oneTimeTokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private EmailService emailService;

    // ---------------------------------------------------------------
    // Pomoćne metode
    // ---------------------------------------------------------------

    private CreateFoundReportRequest validRequest(String email) {
        FoundReportItemRequest wallet;

        wallet = new FoundReportItemRequest(ItemCategory.WALLET, null, null, ItemColor.BLACK,
                null, "koža", null, null, null, null);
        return new CreateFoundReportRequest(
                email, "064 123 4567",
                Instant.now().minus(1, ChronoUnit.HOURS),
                "Niš", "autobus 34, kod Bubnja",
                new BigDecimal("43.320902"), new BigDecimal("21.895759"),
                ItemLocation.WITH_FINDER, null,
                "Šta je bilo u novčaniku osim dokumenata?", "slika ćerke i kartica teretane",
                List.of(wallet));
    }

    // Podnosi nalaz i vraća IZVORNI token iz linka (isti koji bi nalazač dobio na mejl)
    private String submitAndGetRawToken(String email) {
        ArgumentCaptor<String> linkCaptor;
        String link;

        foundReportService.create(validRequest(email));
        linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendFinderAccessLink(anyString(), linkCaptor.capture());
        // getValue() vraća poslednji poziv, a to je upravo ovaj nalaz
        link = linkCaptor.getValue();
        return link.substring(LINK_BASE.length());
    }

    // Odgovor prijave ne sadrži ID, pa se nalaz traži preko heša tokena
    private FoundReport loadReportByRawToken(String rawToken) {
        Long reportId;

        entityManager.flush();
        reportId = jdbcTemplate.queryForObject(
                "SELECT found_report_id FROM one_time_tokens WHERE token_hash = ?", Long.class,
                OneTimeTokenService.hash(rawToken));
        entityManager.clear();
        return foundReportRepository.findById(reportId).orElseThrow();
    }

    // Broj iz tokena može stići kao Integer ili Long, zato se poredi kao long
    private long reportIdFrom(Jwt jwt) {
        Number reportId;

        reportId = jwt.getClaim(FinderTokenService.REPORT_ID_CLAIM);
        return reportId.longValue();
    }

    private void assertInvalidLink(String rawToken) {
        ResponseStatusException exception;

        exception = assertThrows(ResponseStatusException.class, () -> finderAccessService.openLink(rawToken));
        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Link nije ispravan ili je istekao", exception.getReason());
    }

    // ---------------------------------------------------------------
    // Uspešno otvaranje
    // ---------------------------------------------------------------

    @Test
    void openingLinkActivatesReport() {
        String rawToken;
        FoundReport report;
        Long reportId;
        Instant expectedExpiry;

        rawToken = submitAndGetRawToken("nalazac@test.rs");
        report = loadReportByRawToken(rawToken);
        reportId = report.getId();
        assertEquals(FoundReportStatus.PENDING_EMAIL, report.getStatus());

        finderAccessService.openLink(rawToken);

        report = foundReportRepository.findById(reportId).orElseThrow();
        expectedExpiry = Instant.now().plus(60, ChronoUnit.DAYS);
        assertEquals(FoundReportStatus.ACTIVE, report.getStatus());
        assertNotNull(report.getActivatedAt());
        // Novi rok je 60 dana od otvaranja linka (a ne više 24 sata)
        assertTrue(Duration.between(report.getExpiresAt(), expectedExpiry).abs().toMinutes() < 2);
    }

    @Test
    void openingLinkMarksTokenAsUsed() {
        String rawToken;
        Object usedAt;

        rawToken = submitAndGetRawToken("nalazac@test.rs");

        finderAccessService.openLink(rawToken);
        entityManager.flush();

        usedAt = jdbcTemplate.queryForObject(
                "SELECT used_at FROM one_time_tokens WHERE token_hash = ?", Object.class,
                OneTimeTokenService.hash(rawToken));
        assertNotNull(usedAt);
    }

    @Test
    void returnsFinderTokenBoundToThatOneReport() {
        String rawToken;
        Long reportId;
        FinderAccessResponse response;
        Jwt jwt;

        rawToken = submitAndGetRawToken("nalazac@test.rs");
        reportId = loadReportByRawToken(rawToken).getId();

        response = finderAccessService.openLink(rawToken);
        jwt = jwtDecoder.decode(response.accessToken());

        assertEquals(FinderTokenService.FINDER_ROLE, jwt.getClaimAsString("role"));
        assertEquals(reportId.longValue(), reportIdFrom(jwt));
        // Subject nije broj, pa se ne može pomešati sa ID-jem korisnika
        assertEquals("finder", jwt.getSubject());
        assertNotNull(jwt.getId());
        // Token važi oko 2 sata
        assertTrue(Duration.between(Instant.now().plus(120, ChronoUnit.MINUTES), jwt.getExpiresAt()).abs().toMinutes() < 2);
        assertEquals(jwt.getExpiresAt().truncatedTo(ChronoUnit.SECONDS),
                response.expiresAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void eachLinkGivesTokenForItsOwnReport() {
        String firstRawToken;
        String secondRawToken;
        Long firstReportId;
        Long secondReportId;
        Jwt firstJwt;
        Jwt secondJwt;

        firstRawToken = submitAndGetRawToken("prvi@test.rs");
        secondRawToken = submitAndGetRawToken("drugi@test.rs");
        firstReportId = loadReportByRawToken(firstRawToken).getId();
        secondReportId = loadReportByRawToken(secondRawToken).getId();

        firstJwt = jwtDecoder.decode(finderAccessService.openLink(firstRawToken).accessToken());
        secondJwt = jwtDecoder.decode(finderAccessService.openLink(secondRawToken).accessToken());

        assertEquals(firstReportId.longValue(), reportIdFrom(firstJwt));
        assertEquals(secondReportId.longValue(), reportIdFrom(secondJwt));
    }

    // ---------------------------------------------------------------
    // Odbijanje (uvek ista poruka)
    // ---------------------------------------------------------------

    @Test
    void linkWorksOnlyOnce() {
        String rawToken;

        rawToken = submitAndGetRawToken("nalazac@test.rs");

        finderAccessService.openLink(rawToken);

        assertInvalidLink(rawToken);
    }

    @Test
    void unknownTokenIsRejected() {
        assertInvalidLink("ovo-nije-pravi-token-ovo-nije-pravi-token");
    }

    @Test
    void expiredTokenIsRejectedAndReportStaysPending() {
        String rawToken;
        FoundReport report;
        Long reportId;
        String expiredRawToken;

        rawToken = submitAndGetRawToken("nalazac@test.rs");
        report = loadReportByRawToken(rawToken);
        reportId = report.getId();

        // Nov token za isti nalaz, ali koji je istekao pre sat vremena
        expiredRawToken = oneTimeTokenService.issue(OneTimeTokenPurpose.FINDER_ACCESS, report,
                Instant.now().minus(1, ChronoUnit.HOURS));

        assertInvalidLink(expiredRawToken);

        report = foundReportRepository.findById(reportId).orElseThrow();
        assertEquals(FoundReportStatus.PENDING_EMAIL, report.getStatus());
        assertNull(report.getActivatedAt());
    }

    @Test
    void withdrawnReportIsNotActivatedAndTokenStaysUnused() {
        String rawToken;
        FoundReport report;
        Long reportId;
        Object usedAt;

        rawToken = submitAndGetRawToken("nalazac@test.rs");
        report = loadReportByRawToken(rawToken);
        reportId = report.getId();
        report.withdraw(Instant.now());
        foundReportRepository.saveAndFlush(report);
        entityManager.clear();

        assertInvalidLink(rawToken);

        report = foundReportRepository.findById(reportId).orElseThrow();
        assertEquals(FoundReportStatus.WITHDRAWN, report.getStatus());
        entityManager.flush();
        usedAt = jdbcTemplate.queryForObject(
                "SELECT used_at FROM one_time_tokens WHERE token_hash = ?", Object.class,
                OneTimeTokenService.hash(rawToken));
        assertNull(usedAt);
    }

    @Test
    void tokenWithSpacesAroundItStillWorks() {
        String rawToken;

        rawToken = submitAndGetRawToken("nalazac@test.rs");

        // Greška pri kopiranju linka (razmak na kraju) ne sme da obori otvaranje
        assertNotNull(finderAccessService.openLink("  " + rawToken + " ").accessToken());
    }
}