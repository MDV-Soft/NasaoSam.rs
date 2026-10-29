package org.example.nasaosam.auth.service;

import org.example.nasaosam.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt-expiration-minutes}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }
    // pravi potpisan token sa svim podacima za nalog
    public String createToken(User user) {
        Instant now;
        Instant expiresAt;
        JwtClaimsSet claims;
        JwsHeader header;

        now = Instant.now();
        expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        claims = JwtClaimsSet.builder()
                .issuer("nasaosam")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
