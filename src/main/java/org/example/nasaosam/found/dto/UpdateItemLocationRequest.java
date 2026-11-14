package org.example.nasaosam.found.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.nasaosam.found.entity.ItemLocation;

// Nova lokacija predmeta
public record UpdateItemLocationRequest(
        @NotNull(message = "Lokacija predmeta je obavezna")
        ItemLocation itemLocation,

        @Size(max = 255, message = "Podaci o predaji mogu imati najviše 255 znakova")
        String handoverInfo
) {
}