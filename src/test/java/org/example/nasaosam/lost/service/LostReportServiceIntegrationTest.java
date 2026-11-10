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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proverava servis za izgubljene oglase sa pravom bazom, bez kontrolera.
 *
 * @Transactional na test klasi: posle svakog testa sve se poništava (rollback).
 * Anotacije iz DTO-a (@NotNull, @Size...) se ovde ne proveravaju, jer ih pokreće @Valid u kontroleru.
 *
 * MySQL na portu 3307 mora da radi.
 */




//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
// -------------------- CLAUDE PISAO ------------------



@SpringBootTest
@Transactional
class LostReportServiceIntegrationTest {

    // Ispravan SHA-256 format (64 heksadecimalna znaka), kao što šalje frontend
    private static final String CLIENT_NAME_HASH = "ab".repeat(32);
    private static final String CLIENT_NAME_DOB_HASH = "cd".repeat(32);

    private static final String WALLET_DESCRIPTION =
            "Crni kožni novčanik na preklapanje, izlizan donji levi ugao, unutra pregrada sa rajsferšlusom.";

    private static final String KEYS_DESCRIPTION =
            "Tri ključa na metalnom prstenu, jedan od auta, privezak u obliku malog fudbalskog dresa.";

    @Autowired
    private LostReportService lostReportService;

    @Autowired
    private UserRepository userRepository;

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

    // id = null znači nov predmet
    private LostReportItemRequest walletItem(Long id, String description) {
        return new LostReportItemRequest(
                ItemCategory.WALLET, null, null, ItemColor.BLACK,
                "Tommy Hilfiger", "koža", description, "nalepnica sa mačkom", null, null, id);
    }

    private LostReportItemRequest keysItem(Long id) {
        return new LostReportItemRequest(
                ItemCategory.KEYS, null, null, ItemColor.SILVER,
                null, null, KEYS_DESCRIPTION, null, null, null, id);
    }

    private LostReportItemRequest idCardItem(Long id, String nameHash, String nameDobHash) {
        return new LostReportItemRequest(
                ItemCategory.DOCUMENT, null, DocumentType.ID_CARD, null,
                null, null, null, null, nameHash, nameDobHash, id);
    }

    private CreateLostReportRequest request(String city, String privateDetails, List<LostReportItemRequest> items) {
        Instant now;

        now = Instant.now();
        return new CreateLostReportRequest(
                now.minus(2, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS),
                city,
                "autobus 34, smer Bubanj",
                new BigDecimal("43.320902"),
                new BigDecimal("21.895759"),
                300,
                privateDetails,
                items);
    }

    private LostReportResponse createWalletReport(User owner) {
        return lostReportService.create(owner.getId(),
                request("Niš", "slika ćerke", List.of(walletItem(null, WALLET_DESCRIPTION))));
    }

    private int statusOf(ResponseStatusException exception) {
        return exception.getStatusCode().value();
    }

    // ---------------------------------------------------------------
    // Pravljenje
    // ---------------------------------------------------------------

    @Test
    void createsActiveReportWithItemsAndScore() {
        User owner;
        LostReportResponse response;

        owner = createOwner("vlasnik1@test.rs");
        response = createWalletReport(owner);

        assertNotNull(response.id());
        assertEquals(LostReportStatus.ACTIVE, response.status());
        assertEquals(1, response.items().size());
        assertTrue(response.expiresAt().isAfter(Instant.now().plus(89, ChronoUnit.DAYS)));
        assertTrue(response.completenessScore() > 50);
    }

    @Test
    void privateDetailsAreEncryptedInDatabase() {
        User owner;
        LostReportResponse response;
        String valueInDatabase;

        owner = createOwner("vlasnik2@test.rs");
        response = createWalletReport(owner);
        entityManager.flush();

        valueInDatabase = jdbcTemplate.queryForObject(
                "SELECT private_details FROM lost_reports WHERE id = ?", String.class, response.id());

        assertTrue(valueInDatabase.startsWith("v1:"));
        assertFalse(valueInDatabase.contains("ćerke"));
        assertEquals("slika ćerke", lostReportService.getMine(owner.getId(), response.id()).privateDetails());
    }

    @Test
    void nameHashIsStoredAsHmacNotClientHash() {
        User owner;
        LostReportResponse response;
        String hashInDatabase;

        owner = createOwner("vlasnik3@test.rs");
        response = lostReportService.create(owner.getId(),
                request("Niš", null, List.of(idCardItem(null, CLIENT_NAME_HASH, CLIENT_NAME_DOB_HASH))));
        entityManager.flush();

        hashInDatabase = jdbcTemplate.queryForObject(
                "SELECT name_hash FROM lost_report_items WHERE lost_report_id = ?", String.class, response.id());

        assertEquals(64, hashInDatabase.length());
        assertNotEquals(CLIENT_NAME_HASH, hashInDatabase);
        assertTrue(response.items().get(0).hasNameHash());
        assertTrue(response.items().get(0).hasNameDobHash());
    }

    @Test
    void createWithItemIdIsRejected() {
        User owner;
        ResponseStatusException exception;

        owner = createOwner("vlasnik13@test.rs");
        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(),
                        request("Niš", null, List.of(walletItem(999L, WALLET_DESCRIPTION)))));
        assertEquals(400, statusOf(exception));
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
        response = createWalletReport(owner);

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.getMine(otherUser.getId(), response.id()));
        assertEquals(404, statusOf(exception));

        // I izmena i zatvaranje tuđeg oglasa daju 404
        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.close(otherUser.getId(), response.id()));
        assertEquals(404, statusOf(exception));
    }

    @Test
    void listMineReturnsOnlyOwnReports() {
        User owner;
        User otherUser;

        owner = createOwner("vlasnik5@test.rs");
        otherUser = createOwner("drugi5@test.rs");
        createWalletReport(owner);
        createWalletReport(owner);
        createWalletReport(otherUser);

        assertEquals(2, lostReportService.listMine(owner.getId()).size());
        assertEquals(1, lostReportService.listMine(otherUser.getId()).size());
    }

    // ---------------------------------------------------------------
    // Pravila (sva moraju da vrate 400)
    // ---------------------------------------------------------------

    @Test
    void shortDescriptionIsRejected() {
        User owner;
        ResponseStatusException exception;

        owner = createOwner("vlasnik6@test.rs");
        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(),
                        request("Niš", null, List.of(walletItem(null, "crni novčanik")))));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void otherCategoryRequiresCustomName() {
        User owner;
        LostReportItemRequest umbrella;
        ResponseStatusException exception;

        owner = createOwner("vlasnik7@test.rs");
        umbrella = new LostReportItemRequest(ItemCategory.OTHER, null, null, ItemColor.BLACK,
                null, null, WALLET_DESCRIPTION, null, null, null, null);

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), request("Niš", null, List.of(umbrella))));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void nameHashOnWalletIsRejected() {
        User owner;
        LostReportItemRequest walletWithName;
        ResponseStatusException exception;

        owner = createOwner("vlasnik8@test.rs");
        walletWithName = new LostReportItemRequest(ItemCategory.WALLET, null, null, ItemColor.BLACK,
                null, null, WALLET_DESCRIPTION, null, CLIENT_NAME_HASH, null, null);

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), request("Niš", null, List.of(walletWithName))));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void bankCardWithBirthDateIsRejected() {
        User owner;
        LostReportItemRequest bankCard;
        ResponseStatusException exception;

        owner = createOwner("vlasnik9@test.rs");
        bankCard = new LostReportItemRequest(ItemCategory.DOCUMENT, null, DocumentType.BANK_CARD, null,
                null, null, null, null, CLIENT_NAME_HASH, CLIENT_NAME_DOB_HASH, null);

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), request("Niš", null, List.of(bankCard))));
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
                "Niš", null, null, null, null, null, List.of(walletItem(null, WALLET_DESCRIPTION)));

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
                "Niš", null, new BigDecimal("43.320902"), null, null, null,
                List.of(walletItem(null, WALLET_DESCRIPTION)));

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(), halfPoint));
        assertEquals(400, statusOf(exception));
    }

    @Test
    void invalidClientHashIsRejected() {
        User owner;
        ResponseStatusException exception;

        owner = createOwner("vlasnik12@test.rs");
        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.create(owner.getId(),
                        request("Niš", null, List.of(idCardItem(null, "Petar Petrovic", null)))));
        assertEquals(400, statusOf(exception));
    }

    // ---------------------------------------------------------------
    // Izmena
    // ---------------------------------------------------------------

    @Test
    void updateChangesFieldsAndKeepsItemId() {
        User owner;
        LostReportResponse created;
        LostReportResponse updated;
        Long walletId;
        String longerDescription;

        owner = createOwner("izmena1@test.rs");
        created = createWalletReport(owner);
        walletId = created.items().get(0).id();
        longerDescription = WALLET_DESCRIPTION + " Na prednjoj strani utisnut mali logo, šav na jednom mestu popušta.";

        updated = lostReportService.update(owner.getId(), created.id(),
                request("Beograd", "slika ćerke", List.of(walletItem(walletId, longerDescription))));

        assertEquals("Beograd", updated.city());
        assertEquals(1, updated.items().size());
        // Isti predmet je izmenjen, a ne obrisan i napravljen ponovo
        assertEquals(walletId, updated.items().get(0).id());
        assertEquals(longerDescription, updated.items().get(0).description());
    }

    @Test
    void updateAddsNewAndRemovesOmittedItems() {
        User owner;
        LostReportResponse created;
        LostReportResponse updated;

        owner = createOwner("izmena2@test.rs");
        created = createWalletReport(owner);

        // Novčanik se ne šalje (briše se), ključevi bez ID-ja (dodaju se)
        updated = lostReportService.update(owner.getId(), created.id(),
                request("Niš", null, List.of(keysItem(null))));
        entityManager.flush();

        assertEquals(1, updated.items().size());
        assertEquals(ItemCategory.KEYS, updated.items().get(0).category());
        // Nov predmet mora odmah imati ID, da bi frontend mogao da ga menja u sledećoj izmeni
        assertNotNull(updated.items().get(0).id());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lost_report_items WHERE lost_report_id = ?", Integer.class, created.id()));
    }

    @Test
    void updateKeepsNameHashWhenNotResent() {
        User owner;
        LostReportResponse created;
        LostReportResponse updated;
        Long idCardId;

        owner = createOwner("izmena3@test.rs");
        created = lostReportService.create(owner.getId(),
                request("Niš", null, List.of(idCardItem(null, CLIENT_NAME_HASH, CLIENT_NAME_DOB_HASH))));
        idCardId = created.items().get(0).id();

        // Frontend ne zna heš (API ga ne vraća), pa ga ne šalje ponovo
        updated = lostReportService.update(owner.getId(), created.id(),
                request("Niš", null, List.of(idCardItem(idCardId, null, null))));

        assertTrue(updated.items().get(0).hasNameHash());
        assertTrue(updated.items().get(0).hasNameDobHash());
    }

    @Test
    void updateWithItemFromAnotherReportIsRejected() {
        User owner;
        LostReportResponse firstReport;
        LostReportResponse secondReport;
        Long foreignItemId;
        ResponseStatusException exception;

        owner = createOwner("izmena4@test.rs");
        firstReport = createWalletReport(owner);
        secondReport = createWalletReport(owner);
        foreignItemId = secondReport.items().get(0).id();

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.update(owner.getId(), firstReport.id(),
                        request("Niš", null, List.of(walletItem(foreignItemId, WALLET_DESCRIPTION)))));
        assertEquals(400, statusOf(exception));
    }

    // ---------------------------------------------------------------
    // Zatvaranje
    // ---------------------------------------------------------------

    @Test
    void closeSetsStatusAndClosedAt() {
        User owner;
        LostReportResponse created;
        LostReportResponse closed;

        owner = createOwner("zatvaranje1@test.rs");
        created = createWalletReport(owner);
        closed = lostReportService.close(owner.getId(), created.id());

        assertEquals(LostReportStatus.CLOSED, closed.status());
        assertNotNull(closed.closedAt());
        // Zatvoren oglas ostaje u "Mojim oglasima"
        assertEquals(1, lostReportService.listMine(owner.getId()).size());
    }

    @Test
    void closedReportCannotBeClosedOrUpdatedAgain() {
        User owner;
        LostReportResponse created;
        ResponseStatusException exception;

        owner = createOwner("zatvaranje2@test.rs");
        created = createWalletReport(owner);
        lostReportService.close(owner.getId(), created.id());

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.close(owner.getId(), created.id()));
        assertEquals(409, statusOf(exception));

        exception = assertThrows(ResponseStatusException.class,
                () -> lostReportService.update(owner.getId(), created.id(),
                        request("Niš", null, List.of(walletItem(null, WALLET_DESCRIPTION)))));
        assertEquals(409, statusOf(exception));
    }
}