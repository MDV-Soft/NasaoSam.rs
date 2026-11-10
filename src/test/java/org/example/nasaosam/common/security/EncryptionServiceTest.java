package org.example.nasaosam.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
// -------------------- CLAUDE PISAO ------------------
class EncryptionServiceTest {

    // Base64 zapis 32 bajta, isti format kao pravi ključ
    private static final String TEST_KEY = "ZGV2LWFlcy1rbGp1Yy1zYW1vLWxva2Fsbm8tMzJiISE=";

    @Test
    void encryptThenDecryptReturnsOriginal() {
        EncryptionService encryptionService;
        String original;
        String encrypted;

        encryptionService = new EncryptionService(TEST_KEY);
        original = "Slika ćerke i kartica teretane";
        encrypted = encryptionService.encrypt(original);

        assertTrue(encrypted.startsWith("v1:"));
        assertNotEquals(original, encrypted);
        assertEquals(original, encryptionService.decrypt(encrypted));
    }

    @Test
    void sameTextGivesDifferentEncryptedValues() {
        EncryptionService encryptionService;

        encryptionService = new EncryptionService(TEST_KEY);
        // Zbog nasumičnog IV-a, iz baze se ne vidi ni da dva korisnika imaju isti sadržaj
        assertNotEquals(encryptionService.encrypt("isti tekst"), encryptionService.encrypt("isti tekst"));
    }

    @Test
    void tamperedValueIsRejected() {
        EncryptionService encryptionService;
        String encrypted;
        String tampered;
        char originalCharacter;
        char replacement;

        encryptionService = new EncryptionService(TEST_KEY);
        encrypted = encryptionService.encrypt("tajna");

        originalCharacter = encrypted.charAt(10);
        if (originalCharacter == 'A') {
            replacement = 'B';
        } else {
            replacement = 'A';
        }
        tampered = encrypted.substring(0, 10) + replacement + encrypted.substring(11);

        assertThrows(IllegalStateException.class, () -> encryptionService.decrypt(tampered));
    }

    @Test
    void nullStaysNull() {
        EncryptionService encryptionService;

        encryptionService = new EncryptionService(TEST_KEY);
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void wrongKeyLengthIsRejected() {
        assertThrows(IllegalStateException.class, () -> new EncryptionService("MTIzNDU2Nzg5MDEyMzQ1Ng=="));
    }
}