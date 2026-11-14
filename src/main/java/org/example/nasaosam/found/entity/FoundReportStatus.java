package org.example.nasaosam.found.entity;

/**
 * Životni ciklus nalaza. Samo ACTIVE nalazi učestvuju u spajanju.
 * Ceo tok provere vlasnika i plaćanja prati se na poklapanju, ne ovde.
 */
public enum FoundReportStatus {

    // Nalzac je poslao nalaz ali nije otvorio link iz mejla
    PENDING_EMAIL,

    ACTIVE,

    RETURNED,

    EXPIRED,

    WITHDRAWN
}