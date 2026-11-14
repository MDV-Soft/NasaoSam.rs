package org.example.nasaosam.auth.service;


import org.example.nasaosam.auth.entity.OneTimeToken;
import org.example.nasaosam.auth.entity.OneTimeTokenPurpose;
import org.example.nasaosam.auth.repo.OneTimeTokenRepository;
import org.example.nasaosam.found.entity.FoundReport;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Pravi jednokratne tokene za linkove iz mejla.
 *
 * Token se pravi, vraća pozivaocu (da ga stavi u link) i NIGDE se ne čuva u izvornom obliku.
 * U bazi je samo SHA-256 heš. Pri otvaranju linka token iz linka se ponovo hešira i traži po hešu.
 * Zato je dovoljan običan SHA-256, bez ključa: token ima 256 bitova slučajnosti, pa ga nije
 * moguće pogoditi probanjem, za razliku od lozinke ili imena.
 */
@Service
public class OneTimeTokenService {

    // 32 bajta = 256 bitova slučajnosti
    private static final int TOKEN_BYTES = 32;

    private final OneTimeTokenRepository oneTimeTokenRepository;

    // SecureRandom je bezbedan za više niti i pravi nepredvidive vrednosti (običan Random ne sme za tajne)
    private final SecureRandom secureRandom;

    public OneTimeTokenService(OneTimeTokenRepository oneTimeTokenRepository) {
        this.oneTimeTokenRepository = oneTimeTokenRepository;
        this.secureRandom = new SecureRandom();
    }
    // ------------- TODO -------------------
    // PRAVI I CUVA TOKEN, VRACA IZVORNI TOKEN
    public String issue(OneTimeTokenPurpose purpose, FoundReport foundReport, Instant expiresAt) {
        byte[] randomBytes;
        String rawToken;

        randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        oneTimeTokenRepository.save(new OneTimeToken(purpose, hash(rawToken), foundReport, expiresAt));
        return rawToken;
    }

    // SHA-256
    public static String hash(String rawToken) {
        MessageDigest digest;
        byte[] hashBytes;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 nije dostupan", exception);
        }

        hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashBytes);
    }
}