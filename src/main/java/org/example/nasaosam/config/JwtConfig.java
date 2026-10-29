package org.example.nasaosam.config;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {
    //uzima kljuc iz env fajla
    @Value("${app.jwt-secret}")
    private String jwtSecret;
    // hesira kljuc
    @Bean
    public SecretKey jwtSecretKey() {
        byte[] keyBytes;

        keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt-secret mora imati najmanje 32 karaktera");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
    // pravi tokene
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecKey){
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecKey));
    }
    // proverava token na poziv
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecKey){
        return NimbusJwtDecoder.withSecretKey(jwtSecKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
    // heshuje loz pre baze
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
