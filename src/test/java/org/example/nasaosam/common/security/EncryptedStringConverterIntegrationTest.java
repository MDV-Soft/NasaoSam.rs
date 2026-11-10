package org.example.nasaosam.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pokreće celu aplikaciju (zato MySQL na portu 3307 mora da radi)
 * i proverava da Spring ispravno pravi konverter sa ključem iz application.yml.
 */




//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
//TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
// -------------------- CLAUDE PISAO ------------------


@SpringBootTest
class EncryptedStringConverterIntegrationTest {

    @Autowired
    private EncryptedStringConverter encryptedStringConverter;

    @Test
    void converterEncryptsForDatabaseAndDecryptsForJava() {
        String original;
        String valueForDatabase;
        String valueForJava;

        original = "Novčanik: slika ćerke, kartica teretane";

        // Ovo radi Hibernate pri upisu u bazu
        valueForDatabase = encryptedStringConverter.convertToDatabaseColumn(original);

        // Ovo radi Hibernate pri čitanju iz baze
        valueForJava = encryptedStringConverter.convertToEntityAttribute(valueForDatabase);

        assertTrue(valueForDatabase.startsWith("v1:"));
        assertNotEquals(original, valueForDatabase);
        assertEquals(original, valueForJava);
    }
}