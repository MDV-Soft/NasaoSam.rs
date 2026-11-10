package org.example.nasaosam.auth.service;

import jakarta.persistence.EntityManager;
import org.example.nasaosam.auth.dto.AuthResponse;
import org.example.nasaosam.auth.dto.RegisterRequest;
import org.example.nasaosam.user.entity.User;
import org.example.nasaosam.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registracija sa telefonom, na pravoj bazi (MySQL na portu 3307 mora da radi).
 * @Transactional: posle svakog testa sve se poništava, pa test nalozi ne ostaju u bazi.
 */
@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void registrationStoresEncryptedAndNormalizedPhone() {
        AuthResponse response;
        String phoneInDatabase;
        User savedUser;

        response = authService.register(
                new RegisterRequest("telefon1@test.rs", "lozinka123", "Test", "064 123 4567"));
        assertNotNull(response.token());

        // Hibernate još drži izmene u memoriji; flush ih šalje u bazu pre čitanja preko JDBC-a
        entityManager.flush();

        phoneInDatabase = jdbcTemplate.queryForObject(
                "SELECT phone FROM users WHERE email = ?", String.class, "telefon1@test.rs");

        // U bazi: šifrovano, bez ijedne cifre broja
        assertTrue(phoneInDatabase.startsWith("v1:"));
        assertFalse(phoneInDatabase.contains("641234567"));

        // Kroz aplikaciju: dešifrovano i svedeno na jedan oblik
        savedUser = userRepository.findByEmail("telefon1@test.rs").orElseThrow();
        assertEquals("+381641234567", savedUser.getPhone());
    }

    @Test
    void invalidPhoneIsRejectedAndNothingIsSaved() {
        ResponseStatusException exception;

        exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(
                        new RegisterRequest("telefon2@test.rs", "lozinka123", "Test", "0112345678")));
        assertEquals(400, exception.getStatusCode().value());

        // Odbijena registracija ne sme da ostavi nalog
        assertFalse(userRepository.existsByEmail("telefon2@test.rs"));
    }

    @Test
    void duplicateEmailStillReturnsConflict() {
        ResponseStatusException exception;

        authService.register(new RegisterRequest("telefon3@test.rs", "lozinka123", "Test", "064 123 4567"));

        exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(
                        new RegisterRequest("telefon3@test.rs", "lozinka123", "Drugi", "065 765 4321")));
        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void sameTextGivesDifferentEncryptedValuesForTwoUsers() {
        String firstValue;
        String secondValue;

        authService.register(new RegisterRequest("telefon4@test.rs", "lozinka123", "Prvi", "064 123 4567"));
        authService.register(new RegisterRequest("telefon5@test.rs", "lozinka123", "Drugi", "064 123 4567"));
        entityManager.flush();

        firstValue = jdbcTemplate.queryForObject(
                "SELECT phone FROM users WHERE email = ?", String.class, "telefon4@test.rs");
        secondValue = jdbcTemplate.queryForObject(
                "SELECT phone FROM users WHERE email = ?", String.class, "telefon5@test.rs");

        // Isti broj kod dva korisnika izgleda potpuno različito u bazi (nasumičan IV)
        assertNotEquals(firstValue, secondValue);
    }
}