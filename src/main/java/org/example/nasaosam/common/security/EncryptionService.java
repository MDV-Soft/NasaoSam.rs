package org.example.nasaosam.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {
    private static final String VERSION_PREFIX = "v1:";
    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;

    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${app.encryption-key}") String encryptionKeyBase64) {
        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("app.encryption-key mora biti ispravan Base64 zapis", exception);
        }

        // AES-256 trazi 32 bajta
        if (keyBytes.length != 32) {
            throw new IllegalStateException("app.encryption-key mora imati tačno 32 bajta posle Base64 dekodiranja");
        }

        this.secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plainText) {
        byte[] iv;
        byte[] encryptedBytes;
        ByteBuffer buffer;
        Cipher cipher;

        if (plainText == null) {
            return null;
        }

        // Nov random IV za svako sifrovanje: isti tekst nikad ne daje isti rezultat
        iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            // Cipher jedna nit - jedan poziv UVEK!!!!
            cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Šifrovanje nije uspelo", exception);
        }

        // IV prefik nije tajni cuva se zajedno sa sifrovanim tekstom da bi desifrovanje bilo moguce
        buffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
        buffer.put(iv);
        buffer.put(encryptedBytes);

        return VERSION_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
    }

    public String decrypt(String storedValue) {
        byte[] allBytes;
        byte[] iv;
        byte[] encryptedBytes;
        byte[] plainBytes;
        Cipher cipher;

        if (storedValue == null) {
            return null;
        }

        if (!storedValue.startsWith(VERSION_PREFIX)) {
            throw new IllegalStateException("Nepoznat format šifrovane vrednosti");
        }

        allBytes = Base64.getDecoder().decode(storedValue.substring(VERSION_PREFIX.length()));

        if (allBytes.length <= IV_LENGTH_BYTES) {
            throw new IllegalStateException("Šifrovana vrednost je oštećena");
        }

        iv = new byte[IV_LENGTH_BYTES];
        encryptedBytes = new byte[allBytes.length - IV_LENGTH_BYTES];
        System.arraycopy(allBytes, 0, iv, 0, IV_LENGTH_BYTES);
        System.arraycopy(allBytes, IV_LENGTH_BYTES, encryptedBytes, 0, encryptedBytes.length);

        try {
            cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            // GCM proverava tag tjst ako je neko menjao vrednost u bazi, ovde pada
            plainBytes = cipher.doFinal(encryptedBytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Dešifrovanje nije uspelo: pogrešan ključ ili izmenjena vrednost", exception);
        }

        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}

