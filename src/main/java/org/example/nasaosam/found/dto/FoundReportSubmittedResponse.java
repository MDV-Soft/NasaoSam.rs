package org.example.nasaosam.found.dto;

import java.time.Instant;

/**
 * Odgovor na prijavu nalaza. Namerno ne sadrži ni ID nalaza, ni token, ni kontakt podatke:
 * ruta je javna, pa odgovor ne sme krije nist
 */
public record FoundReportSubmittedResponse(
        String message,
        Instant linkExpiresAt
) {
}