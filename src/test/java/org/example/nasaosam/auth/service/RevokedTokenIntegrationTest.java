package org.example.nasaosam.auth.service;

import org.example.nasaosam.user.entity.Role;
import org.example.nasaosam.user.entity.User;
import org.example.nasaosam.user.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Odjava i opoziv tokena, na pravoj bazi (MySQL na portu 3307 mora da radi).
 * Proverava ceo lanac: token dobija ID, odjava ga upisuje u spisak, a dekoder ga posle toga odbija.
 * @Transactional: posle svakog testa sve se poništava.
 */
@SpringBootTest
@Transactional
class RevokedTokenIntegrationTest {

    @Autowired
    private RevokedTokenService revokedTokenService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String createToken(String email) {
        User user;

        user = userRepository.save(new User(email, "hash-nije-bitan-za-test", "Test", Role.OWNER));
        return jwtService.createToken(user);
    }

    // Token potpisan našim ključem, ali sa claim-ovima po izboru testa
    private String signToken(JwtClaimsSet claims) {
        JwsHeader header;

        header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    void everyTokenGetsItsOwnId() {
        String firstToken;
        String secondToken;
        Jwt firstJwt;
        Jwt secondJwt;

        firstToken = createToken("odjava1@test.rs");
        secondToken = createToken("odjava2@test.rs");
        firstJwt = jwtDecoder.decode(firstToken);
        secondJwt = jwtDecoder.decode(secondToken);

        assertNotNull(firstJwt.getId());
        assertNotNull(secondJwt.getId());
        assertNotEquals(firstJwt.getId(), secondJwt.getId());
    }

    @Test
    void freshTokenIsAccepted() {
        Jwt jwt;

        jwt = jwtDecoder.decode(createToken("odjava3@test.rs"));

        assertEquals("OWNER", jwt.getClaimAsString("role"));
    }

    @Test
    void tokenIsRejectedAfterLogout() {
        String token;
        Jwt jwt;

        token = createToken("odjava4@test.rs");
        jwt = jwtDecoder.decode(token);

        authService.logout(jwt);

        assertThrows(JwtValidationException.class, () -> jwtDecoder.decode(token));
    }

    @Test
    void logoutOfOneTokenDoesNotAffectAnother() {
        String firstToken;
        String secondToken;

        firstToken = createToken("odjava5@test.rs");
        secondToken = createToken("odjava6@test.rs");

        authService.logout(jwtDecoder.decode(firstToken));

        assertThrows(JwtValidationException.class, () -> jwtDecoder.decode(firstToken));
        // Drugi token (npr. ista osoba prijavljena na drugom uređaju) i dalje radi
        assertNotNull(jwtDecoder.decode(secondToken));
    }

    @Test
    void revokingTheSameTokenTwiceIsHarmless() {
        String tokenId;
        Instant expiresAt;
        Integer rowCount;

        tokenId = "11111111-2222-3333-4444-555555555555";
        expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        revokedTokenService.revoke(tokenId, expiresAt);
        revokedTokenService.revoke(tokenId, expiresAt);

        rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM revoked_tokens WHERE jti = ?", Integer.class, tokenId);
        assertEquals(1, rowCount);
        assertTrue(revokedTokenService.isRevoked(tokenId));
    }

    @Test
    void unknownTokenIsNotRevoked() {
        assertFalse(revokedTokenService.isRevoked("99999999-9999-9999-9999-999999999999"));
    }

    @Test
    void tokenWithoutIdIsRejected() {
        Instant now;
        String token;

        // Kao tokeni izdati pre uvođenja odjave: ispravni, ali bez ID-ja
        now = Instant.now();
        token = signToken(JwtClaimsSet.builder()
                .issuer("nasaosam")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject("7")
                .claim("role", "OWNER")
                .build());

        assertThrows(JwtValidationException.class, () -> jwtDecoder.decode(token));
    }

    @Test
    void expiredTokenIsStillRejected() {
        Instant now;
        String token;

        // Dokaz da je ugrađena provera isteka ostala uz našu (setJwtValidator je zamenjuje, ne dopunjuje)
        now = Instant.now();
        token = signToken(JwtClaimsSet.builder()
                .issuer("nasaosam")
                .issuedAt(now.minus(20, ChronoUnit.MINUTES))
                .expiresAt(now.minus(10, ChronoUnit.MINUTES))
                .subject("7")
                .id("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
                .claim("role", "OWNER")
                .build());

        assertThrows(JwtValidationException.class, () -> jwtDecoder.decode(token));
    }
}