package org.example.nasaosam.found.dto;

import org.example.nasaosam.found.entity.FoundReportStatus;
import org.example.nasaosam.found.entity.ItemLocation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Nalaz onako kako ga vidi nalazač.
 */
public record FinderReportResponse(
        FoundReportStatus status,
        Instant foundAt,
        String city,
        String placeDescription,
        BigDecimal latitude,
        BigDecimal longitude,
        ItemLocation itemLocation,
        String handoverInfo,
        String controlQuestion,
        Instant activatedAt,
        Instant expiresAt,
        List<FinderReportItemResponse> items
) {
}