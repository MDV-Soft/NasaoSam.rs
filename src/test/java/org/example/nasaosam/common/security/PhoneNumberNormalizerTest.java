package org.example.nasaosam.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberNormalizerTest {

    private final PhoneNumberNormalizer normalizer = new PhoneNumberNormalizer();

    @Test
    void differentWaysOfWritingTheSameNumberGiveSameResult() {
        String[] variants;
        String expected;

        expected = "+381641234567";
        variants = new String[]{
                "064 123 4567",
                "0641234567",
                "064/123-4567",
                "+381 64 123 4567",
                "+381641234567",
                "00381641234567",
                "381641234567",
                "(064) 123.4567",
                "  064 1234567  "
        };

        for (int index = 0; index < variants.length; index++) {
            assertEquals(expected, normalizer.normalize(variants[index]), "Oblik: " + variants[index]);
        }
    }

    @Test
    void shorterMobileNumberIsAccepted() {
        // Neki mobilni brojevi imaju devet cifara sa nulom (060 123 456)
        assertEquals("+38160123456", normalizer.normalize("060 123 456"));
    }

    @Test
    void invalidNumbersAreRejected() {
        String[] invalidNumbers;

        invalidNumbers = new String[]{
                "",                 // prazno
                "   ",              // samo razmaci
                "12345",            // prekratko
                "0112345678",       // fiksni broj (Beograd), ne mobilni
                "064 123",          // prekratko
                "064 123 456789",   // predugačko
                "+385 91 123 4567", // strani broj
                "abc",              // slova
                "064-12a-4567"      // slovo usred broja
        };

        for (int index = 0; index < invalidNumbers.length; index++) {
            String invalidNumber = invalidNumbers[index];
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> normalizer.normalize(invalidNumber), "Trebalo je odbiti: " + invalidNumber);
            assertEquals(400, exception.getStatusCode().value());
        }
    }

    @Test
    void nullIsRejected() {
        assertThrows(ResponseStatusException.class, () -> normalizer.normalize(null));
    }
}