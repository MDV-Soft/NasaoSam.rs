package org.example.nasaosam.lost.service;

import jakarta.persistence.EntityManager;
import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;
import org.example.nasaosam.lost.dto.CreateLostReportRequest;
import org.example.nasaosam.lost.dto.LostReportItemRequest;
import org.example.nasaosam.lost.dto.LostReportResponse;
import org.example.nasaosam.lost.entity.LostReportStatus;
import org.example.nasaosam.user.entity.Role;
import org.example.nasaosam.user.entity.User;
import org.example.nasaosam.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
// -------------------- CLAUDE PISAO ------------------

/**
 * Proverava ceo korak 4 sa pravom bazom, bez kontrolera.
 *
 * @Transactional na test klasi: posle svakog testa sve se vraća (rollback),
 * pa test korisnici i oglasi ne ostaju u bazi.
 *
 * Napomena: anotacije iz DTO-a (@NotNull, @Size...) se ovde ne proveravaju,
 * jer ih pokreće @Valid u kontroleru. Ovde se testiraju pravila iz servisa.
 *
 * MySQL na portu 3307 mora da radi.
 */
@SpringBootTest
@Transactional
class LostReportServiceIntegrationTest {

    // Ispravan SHA-256 format (64 heksadecimalna znaka), kao što šalje frontend
    private static final String CLIENT_NAME_HASH = "ab".repeat(32);
    private static final String CLIENT_NAME_DOB_HASH = "cd".repeat(32);

    private static final String WALLET_DESCRIPTION =
            "Crni kožni novčanik na preklapanje, izlizan donji levi ugao, unutra pregrada sa rajsferšlusom.";

    @Autowired
    private LostReportService lostReportService;

    @Autowired
    private UserRepository userRepository;

    // Za čitanje direktno iz baze, mimo Hibernate-a, da vidimo šta je stvarno upisano
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    // ---------------------------------------------------------------
    // Pomoćne metode za pravljenje podataka
    // ---------------------------------------------------------------

    private User createOwner(String email) {
        return userRepository.save(new User(email, "hash-nije-bitan-za-test", "Test", Role.OWNER));
    }

    private LostReportItemRequest walletItem(String description) {
        return new LostReportItemRequest(
                ItemCategory.WALLET, null, null, ItemColor.BLACK,
                "Tommy Hilfiger", "koža", description, "nalepnica sa mačkom", null, null);
    }

    private LostReportItemRequest idCardItem(String nameHash, String nameDobHash) {
        return new LostReportItemRequest(
                ItemCategory.DOCUMENT, null, DocumentType.ID_CARD, null,
                null, null, null, null, nameHash, nameDobHash);
    }

    private CreateLostReportRequest request(String privateDetails, List<LostReportItemRequest> items) {
        Instant now;

        now = Instant.now();
        return new CreateLostReportRequest(
                now.minus(2, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS),
                "Niš",
                "autobus 34, smer Bubanj",
                new BigDecimal("43.320902"),
                new BigDecimal("21.895759"),
                300,
                privateDetails,
                items);
    }

    private int statusOf(ResponseStatusException exception) {
        return exception.getStatusCode().value();
    }

    // ---------------------------------------------------------------
    // Uspešno pravljenje
    // ---------------------------------------------------------------

    @Test
    void createsActiveReportWithItemsAndScore() {
        User owner;
        LostReportResponse response;

        owner = createOwner("vlasnik1@test.rs");
        response = lostReportService.create(owner.getId(), request("slika ćerke", List.of(walletItem(WALLET_DESCRIPTION))));

        assertTrue(response.id() != null);
        assertEquals(LostReportStatus.ACTIVE, response.status());
        assertEquals(1, response.items().size());
        assertEquals("Niš", response.city());
        // Rok isteka je oko 90 dana od sada
        assertTrue(response.expiresAt().isAfter(Instant.now().plus(89, ChronoUnit.DAYS)));
        // Oglas je skoro kompletan, pa ocena mora biti visoka
        assertTrue(response.completenessScore() > 50);
    }

    @Test
    void privateDetailsAreEncryptedInDatabase() {
        User owner;
        LostReportResponse response;
        String valueInDatabase;

        owner = createOwner("vlasnik2@test.rs");
        response = lostReportService.create(owner.getId(), request("slika ćerke", List.of(walletItem(WALLET_DESCRIPTION))));

        // Hibernate još drži izmene u memoriji; flush ih šalje u bazu pre čitanja preko JDBC-a
        entityManager.flush();

        valueInDatabase = jdbcTemplate.queryForObject(
                "SELECT private_details FROM lost_reports WHERE id = ?", String.class, response.id());

        // U bazi: šifrovano
        assertTrue(valueInDatabase.startsWith("v1:"));
        assertFalse(valueInDatabase.contains("ćerke"));

        // Kroz servis: običan tekst
        assertEquals("slika ćerke", lostReportService.getMine(owner.getId(), response.id()).privateDetails());
    }

    @Test
    void nameHashIsStoredAsHmacNotClientHash() {
        User owner;
        LostReportResponse response;
        String hashInDatabase;

        owner = createOwner("vlasnik3@test.rs");
        response = lostReportService.create(owner.getId(),
                request(null, List.of(idCardItem(CLIENT_NAME_HASH, CLIENT_NAME_DOB_HASH))));

        entityManager.flush();

        hashInDatabase = jdbcTemplate.queryForObject(
                "SELECT name_hash FROM lost_report_items WHERE lost_report_id = ?", String.class, response.id());

        // U bazi je HMAC: 64 znaka, ali različit od onoga što je poslao telefon
        assertEquals(64, hashInDatabase.length());
        assertNotEquals(CLIENT_NAME_HASH, hashInDatabase);

        // API ne vraća heš, samo informaciju da postoji
        assertTrue(response.items().get(0).hasNameHash());
        assertTrue(response.items().get(0).hasNameDobHash());
    }

    // ---------------------------------------------------------------
    // Zaštita tuđih oglasa
    // ---------------------------------------------------------------

    @Test
    void otherOwnerGetsNotFound() {
        User owner;
        User otherUser;
        LostReportResponse response;
        ResponseStatusException exception;

        owner = createOwner("vlasnik4@test.rs");
        otherUser = createOwner("drugi4@test.rs");
        response = lostReportService.create(owner.getId(), request(null, List.of(walletItem(WALLET_DESCRIPTION))));

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.getMine(otherUser.getId(), response.id()));
        assertEquals(404, statusOf(exception));
    }

    @Test
    void listMineReturnsOnlyOwnReports() {
        User owner;
        User otherUser;

        owner = createOwner("vlasnik5@test.rs");
        otherUser = createOwner("drugi5@test.rs");
        lostReportService.create(owner.getId(), request(null, List.of(walletItem(WALLET_DESCRIPTION))));
        lostReportService.create(owner.getId(), request(null, List.of(walletItem(WALLET_DESCRIPTION))));
        lostReportService.create(otherUser.getId(), request(null, List.of(walletItem(WALLET_DESCRIPTION))));

        assertEquals(2, lostReportService.listMine(owner.getId()).size());
        assertEquals(1, lostReportService.listMine(otherUser.getId()).size());
    }

    // ---------------------------------------------------------------
    // Pravila iz servisa (sva moraju da vrate 400)
    // ---------------------------------------------------------------

    @Test
    void shortDescriptionIsRejected() {
        User owner;
        ResponseStatusException exception;

        owner = createOwner("vlasnik6@test.rs");
        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), request(null, List.of(walletItem("crni novčanik")))));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void otherCategoryRequiresCustomName() {
        User owner;
        LostReportItemRequest umbrella;
        ResponseStatusException exception;

        owner = createOwner("vlasnik7@test.rs");
        umbrella = new LostReportItemRequest(ItemCategory.OTHER, null, null, ItemColor.BLACK,
                null, null, WALLET_DESCRIPTION, null, null, null);

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), request(null, List.of(umbrella))));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void nameHashOnWalletIsRejected() {
        User owner;
        LostReportItemRequest walletWithName;
        ResponseStatusException exception;

        owner = createOwner("vlasnik8@test.rs");
        walletWithName = new LostReportItemRequest(ItemCategory.WALLET, null, null, ItemColor.BLACK,
                null, null, WALLET_DESCRIPTION, null, CLIENT_NAME_HASH, null);

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), request(null, List.of(walletWithName))));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void bankCardWithBirthDateIsRejected() {
        User owner;
        LostReportItemRequest bankCard;
        ResponseStatusException exception;

        owner = createOwner("vlasnik9@test.rs");
        bankCard = new LostReportItemRequest(ItemCategory.DOCUMENT, null, DocumentType.BANK_CARD, null,
                null, null, null, null, CLIENT_NAME_HASH, CLIENT_NAME_DOB_HASH);

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), request(null, List.of(bankCard))));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void reversedTimeRangeIsRejected() {
        User owner;
        Instant now;
        CreateLostReportRequest reversed;
        ResponseStatusException exception;

        owner = createOwner("vlasnik10@test.rs");
        now = Instant.now();
        reversed = new CreateLostReportRequest(
                now.minus(1, ChronoUnit.HOURS), now.minus(2, ChronoUnit.HOURS),
                "Niš", null, null, null, null, null, List.of(walletItem(WALLET_DESCRIPTION)));

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), reversed));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void latitudeWithoutLongitudeIsRejected() {
        User owner;
        Instant now;
        CreateLostReportRequest halfPoint;
        ResponseStatusException exception;

        owner = createOwner("vlasnik11@test.rs");
        now = Instant.now();
        halfPoint = new CreateLostReportRequest(
                now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS),
                "Niš", null, new BigDecimal("43.320902"), null, null, null, List.of(walletItem(WALLET_DESCRIPTION)));

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), halfPoint));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void invalidClientHashIsRejected() {
        User owner;
        ResponseStatusException exception;

        owner = createOwner("vlasnik12@test.rs");

        // Pravo ime umesto heša: HmacService mora da odbije
        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), request(null, List.of(idCardItem("Petar Petrovic", null)))));
        assertEquals(400, statusOf(exception));
    }
}