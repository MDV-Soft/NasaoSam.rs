package org.example.nasaosam.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// TEST  TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
//  TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
class HmacServiceTest {

    private static final String TEST_SECRET = "test-tajna-za-hmac-najmanje-32-karaktera";

    private static final String HASH_PETAR = "7b1c0f2c9e3a4d5b6c7d8e9f0a1b2c3d4e5f60718293a4b5c6d7e8f9a0b1c2d3";
    private static final String HASH_MARKO = "0a1b2c3d4e5f60718293a4b5c6d7e8f9a0b1c2d37b1c0f2c9e3a4d5b6c7d8e9f";

    @Test
    void sameInputGivesSameResult() {
        HmacService hmacService;
        String firstResult;
        String secondResult;

        hmacService = new HmacService(TEST_SECRET);
        firstResult = hmacService.hashClientHash(HASH_PETAR);
        secondResult = hmacService.hashClientHash(HASH_PETAR);

        assertEquals(firstResult, secondResult);
        assertEquals(64, firstResult.length());
    }

    @Test
    void differentInputGivesDifferentResult() {
        HmacService hmacService;

        hmacService = new HmacService(TEST_SECRET);
        assertNotEquals(hmacService.hashClientHash(HASH_PETAR), hmacService.hashClientHash(HASH_MARKO));
    }

    @Test
    void differentSecretGivesDifferentResult() {
        HmacService firstService;
        HmacService secondService;

        firstService = new HmacService(TEST_SECRET);
        secondService = new HmacService("druga-tajna-za-hmac-najmanje-32-karaktera");

        assertNotEquals(firstService.hashClientHash(HASH_PETAR), secondService.hashClientHash(HASH_PETAR));
    }

    @Test
    void invalidInputIsRejected() {
        HmacService hmacService;

        hmacService = new HmacService(TEST_SECRET);
        assertThrows(ResponseStatusException.class, () -> hmacService.hashClientHash("Petar Petrovic"));
        assertThrows(ResponseStatusException.class, () -> hmacService.hashClientHash("abc123"));
        assertThrows(ResponseStatusException.class, () -> hmacService.hashClientHash(HASH_PETAR.toUpperCase()));
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalStateException.class, () -> new HmacService("kratka"));
    }
}