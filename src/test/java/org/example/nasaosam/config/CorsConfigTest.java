package org.example.nasaosam.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void splitsAndTrimsOrigins() {
        CorsConfiguration configuration;

        configuration = new CorsConfig(" http://localhost:4200 , https://nasaosam.netlify.app ").buildConfiguration();

        assertEquals(2, configuration.getAllowedOrigins().size());
        assertTrue(configuration.getAllowedOrigins().contains("http://localhost:4200"));
        assertTrue(configuration.getAllowedOrigins().contains("https://nasaosam.netlify.app"));
    }

    @Test
    void neverAllowsEveryOrigin() {
        CorsConfiguration configuration;

        configuration = new CorsConfig("http://localhost:4200").buildConfiguration();

        assertFalse(configuration.getAllowedOrigins().contains("*"));
    }

    @Test
    void allowsMethodsAndHeadersTheApiUses() {
        CorsConfiguration configuration;

        configuration = new CorsConfig("http://localhost:4200").buildConfiguration();

        assertTrue(configuration.getAllowedMethods().contains("PUT"));
        assertTrue(configuration.getAllowedMethods().contains("DELETE"));
        assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
        assertTrue(configuration.getAllowedHeaders().contains("Authorization"));
        assertTrue(configuration.getAllowedHeaders().contains("Content-Type"));
    }

    @Test
    void exposesLocationHeader() {
        CorsConfiguration configuration;

        configuration = new CorsConfig("http://localhost:4200").buildConfiguration();

        assertTrue(configuration.getExposedHeaders().contains("Location"));
    }

    @Test
    void emptyOriginsAreRejected() {
        assertThrows(IllegalStateException.class, () -> new CorsConfig("  , ,").buildConfiguration());
    }
}