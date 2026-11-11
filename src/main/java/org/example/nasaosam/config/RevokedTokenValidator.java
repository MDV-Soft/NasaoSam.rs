package org.example.nasaosam.config;

import org.example.nasaosam.auth.service.RevokedTokenService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Dodatna provera tokena pri svakom zahtevu: token mora imati ID i ne sme biti opozvan.
 * Ako provera ne prođe, zahtev dobija 401, kao da token nije ispravan.
 *
 * Token bez ID-ja se odbija jer se ne bi mogao opozvati. Takvi su samo tokeni izdati
 * pre uvođenja odjave; oni važe najviše 24 sata, a korisnik se samo prijavi ponovo.
 */
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final RevokedTokenService revokedTokenService;

    public RevokedTokenValidator(RevokedTokenService revokedTokenService) {
        this.revokedTokenService = revokedTokenService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenId;
        OAuth2Error error;

        tokenId = jwt.getId();
        if (tokenId == null) {
            error = new OAuth2Error("invalid_token", "Token nema identifikator", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        if (revokedTokenService.isRevoked(tokenId)) {
            error = new OAuth2Error("invalid_token", "Token je opozvan", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        return OAuth2TokenValidatorResult.success();
    }
}