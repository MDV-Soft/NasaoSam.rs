package org.example.nasaosam.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Stavlja se na polje entiteta sa @Convert(converter = EncryptedStringConverter.class).
 * Ostatak koda radi sa obicnim tekstom, a u bazi je uvek sifrovana vrednost.
 *
 * BITNO _________ po sifrovanom polju ne moze da se pretrazuje ni sortira u SQL-u.
 * Zato se sifruju samo polja koja nam ne trebaju za pretragu.
 */

@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    private final EncryptionService encryptionService;

    public EncryptedStringConverter(EncryptionService encryptionService){this.encryptionService = encryptionService;}

    @Override
    public String convertToDatabaseColumn(String attr){
        return encryptionService.encrypt(attr);
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        return encryptionService.decrypt(dbValue);
    }
}
