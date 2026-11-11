package org.example.nasaosam.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.example.nasaosam.auth.service.RevokedTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    @Value("${app.jwt-secret}")
    private String jwtSecret;

    // Tajni ključ za potpisivanje tokena (HS256 traži najmanje 32 bajta)
    @Bean
    public SecretKey jwtSecretKey() {
        byte[] keyBytes;

        keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt-secret mora imati najmanje 32 karaktera");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    // Pravi (potpisuje) tokene pri prijavi
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /**
     * Proverava tokene u svakom zahtevu.
     *
     * Provere idu ovim redom:
     *   1. ugrađene: potpis i vreme isteka (JwtValidators.createDefault),
     *   2. naša: token ima ID i nije opozvan pri odjavi (RevokedTokenValidator).
     *
     * Bez prve provere (setJwtValidator je zamenjuje, ne dopunjuje) istekli tokeni bi važili zauvek,
     * zato je ona ovde izričito navedena.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, RevokedTokenService revokedTokenService) {
        NimbusJwtDecoder decoder;

        decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new RevokedTokenValidator(revokedTokenService)));

        return decoder;
    }

    // Hešuje lozinke pre čuvanja u bazi
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}