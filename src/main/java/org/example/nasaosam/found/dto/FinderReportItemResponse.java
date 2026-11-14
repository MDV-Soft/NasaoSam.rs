package org.example.nasaosam.found.dto;

import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;

// Predmet iz nalaza onako kako ga nalazac vidi
public record FinderReportItemResponse(
        ItemCategory category,
        String customCategory,
        DocumentType documentType,
        ItemColor color,
        String brand,
        String material,
        String description,
        String distinctiveMarks
) {
}