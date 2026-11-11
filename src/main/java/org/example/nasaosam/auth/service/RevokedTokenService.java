package org.example.nasaosam.auth.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Spisak opozvanih tokena.
 *
 * ZASTO POSTOJI
 *   JWT vazi sam od sebe: server ga ne pamti, pa ga ne može orbisati. Zato se pri odjavi
 *   upisuje ID tokena (jti) u ovaj spisak, a svaki zahtev proverava da njegov token nije na njemu.
 */
@Service
public class RevokedTokenService {

    private final JdbcTemplate jdbcTemplate;

    public RevokedTokenService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Opoziva token. Ako je već opozvan, ništa se ne desi (nema greške ni duplikata).
     *
     * ON DUPLICATE KEY UPDATE: dve istovremene odjave istim tokenom ne smeju da daju grešku.
     */
    public void revoke(String tokenId, Instant expiresAt) {
        LocalDateTime expiresAtUtc;

        expiresAtUtc = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO revoked_tokens (jti, expires_at) VALUES (?, ?) ON DUPLICATE KEY UPDATE jti = jti",
                tokenId, expiresAtUtc);
    }

    // Poziva se pri svakom zahtevu sa tokenom
    public boolean isRevoked(String tokenId) {
        Integer count;

        count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM revoked_tokens WHERE jti = ?", Integer.class, tokenId);
        if (count == null) {
            return false;
        }
        return count > 0;
    }
}