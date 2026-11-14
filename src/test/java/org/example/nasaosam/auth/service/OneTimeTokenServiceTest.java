package org.example.nasaosam.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

// Heširanje tokena, bez baze i Spring-a
class OneTimeTokenServiceTest {

    @Test
    void hashMatchesKnownSha256Value() {
        // Poznata vrednost iz specifikacije SHA-256: heš teksta "abc"
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                OneTimeTokenService.hash("abc"));
    }

    @Test
    void sameTokenGivesSameHash() {
        // Bez ovoga link ne bi mogao da se pronađe po hešu
        assertEquals(OneTimeTokenService.hash("neki-token"), OneTimeTokenService.hash("neki-token"));
    }

    @Test
    void differentTokensGiveDifferentHashes() {
        assertNotEquals(OneTimeTokenService.hash("token-1"), OneTimeTokenService.hash("token-2"));
    }

    @Test
    void hashHasSixtyFourLowercaseHexCharacters() {
        String hash;

        hash = OneTimeTokenService.hash("bilo-koji-token");

        // Isto što traži kolona token_hash CHAR(64)
        assertEquals(64, hash.length());
        assertEquals(hash.toLowerCase(), hash);
    }
}