package org.example.nasaosam.auth.service;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class FinderTokenService {
    public static final String FINDER_ROLE = "FINDER";
    public static final String FINDER_SUBJECT = "finder";
    public static final String REPORT_ID_CLAIM = "reportId";
    private final JwtEncoder jwtEncoder;

    public FinderTokenService(JwtEncoder jwtEncoder){
        this.jwtEncoder = jwtEncoder;
    }

    public String createToken(Long reportId, Instant issuedAt, Instant expiresAt){
        JwtClaimsSet claimsSet;
        JwsHeader header;

        claimsSet = JwtClaimsSet.builder()
                .issuer("nasaosam")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(FINDER_SUBJECT)
                .id(UUID.randomUUID().toString())
                .claim(REPORT_ID_CLAIM, reportId)
                .claim("role", FINDER_ROLE)
                .build();

        header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claimsSet)).getTokenValue();
    }
}
