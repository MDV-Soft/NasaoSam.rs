package org.example.nasaosam.found.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.nasaosam.common.item.DocumentType;
import org.example.nasaosam.common.item.ItemCategory;
import org.example.nasaosam.common.item.ItemColor;


public record FoundReportItemRequest(

        @NotNull(message = "Vrsta predmeta je obavezna")
        ItemCategory category,

        @Size(max = 50, message = "Naziv predmeta može imati najviše 50 znakova")
        String customCategory,

        DocumentType documentType,

        ItemColor color,

        @Size(max = 100, message = "Marka može imati najviše 100 znakova")
        String brand,

        @Size(max = 50, message = "Materijal može imati najviše 50 znakova")
        String material,

        @Size(max = 2000, message = "Opis može imati najviše 2000 znakova")
        String description,

        @Size(max = 500, message = "Posebni znaci mogu imati najviše 500 znakova")
        String distinctiveMarks,

        // SHA-256 sa telefona
        String nameHash,

        String nameDobHash
) {
}