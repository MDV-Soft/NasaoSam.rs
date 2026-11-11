package org.example.nasaosam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS: dozvola browseru da frontend sa jedne adrese (npr. http://localhost:4200)
 * zove backend na drugoj adresi (http://localhost:8080).
 *
 * ZAŠTO JE POTREBNO
 *   Browser smatra da su dve adrese "različito poreklo" (origin) čim se razlikuje bar port.
 *   Pre svakog zahteva sa tokenom browser pošalje "preflight" (OPTIONS) i pita backend
 *   "da li smem?". Bez odgovora sa ispravnim zaglavljima, browser blokira poziv.
 *   Postman ovo ne radi jer nije browser, pa su svi dosadašnji testovi prolazili bez CORS-a.
 *
 * DOZVOLJENE ADRESE
 *   Dolaze iz app.cors-allowed-origins (zarezom odvojene). Lokalno je to samo Angular dev server,
 *   a u produkciji se kroz promenljivu okruženja postavlja pravi domen frontenda.
 *   Namerno nema "*": dozvola svima bi značila da bilo koji sajt može da zove API
 *   iz browsera prijavljenog korisnika.
 *
 * Nema kolačića ni sesija (JWT ide u Authorization zaglavlju), pa "allowCredentials" ostaje isključen.
 */
@Configuration
public class CorsConfig {

    private final String allowedOrigins;

    public CorsConfig(@Value("${app.cors-allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source;

        source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildConfiguration());
        return source;
    }

    CorsConfiguration buildConfiguration() {
        CorsConfiguration configuration;
        List<String> origins;
        String[] parts;
        String part;

        origins = new ArrayList<>();
        parts = allowedOrigins.split(",");
        for (int index = 0; index < parts.length; index++) {
            part = parts[index].trim();
            if (!part.isEmpty()) {
                origins.add(part);
            }
        }

        if (origins.isEmpty()) {
            throw new IllegalStateException("app.cors-allowed-origins mora sadržati bar jednu adresu");
        }

        configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        configuration.setExposedHeaders(List.of("Location"));

        configuration.setMaxAge(3600L);
        return configuration;
    }
}