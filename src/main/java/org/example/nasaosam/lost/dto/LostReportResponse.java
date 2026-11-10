package org.example.nasaosam.lost.dto;

import org.example.nasaosam.lost.entity.LostReportStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Ono sto vlasnik vidi o svom oglasu. ovo ide ide samo vlasniku,
 * zato sme da sadrzi privatne detalje
 */
public record LostReportResponse(
        Long id,
        LostReportStatus status,
        Instant lostFrom,
        Instant lostTo,
        String city,
        String placeDescription,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer locationRadiusMeters,
        String privateDetails,
        int completenessScore,
        Instant expiresAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        List<LostReportItemResponse> items
) {
}