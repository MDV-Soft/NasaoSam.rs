package org.example.nasaosam.lost.dto;

import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;

/**
 * Hesevi imena se nikad ne vracju  kroz APi ni vlasniku.
 * Frontendu znqa samo dal postoje
 */
public record LostReportItemResponse(
        Long id,
        ItemCategory category,
        String customCategory,
        DocumentType documentType,
        ItemColor color,
        String brand,
        String material,
        String description,
        String distinctiveMarks,
        boolean hasNameHash,
        boolean hasNameDobHash
) {
}