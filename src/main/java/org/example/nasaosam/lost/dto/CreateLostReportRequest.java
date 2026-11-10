package org.example.nasaosam.lost.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
// VALIDATORIIIIIIIIII
public record CreateLostReportRequest(

        @NotNull(message = "Početak vremena gubitka je obavezan")
        Instant lostFrom,

        @NotNull(message = "Kraj vremena gubitka je obavezan")
        Instant lostTo,

        @NotBlank(message = "Grad je obavezan")
        @Size(max = 100, message = "Grad može imati najviše 100 znakova")
        String city,

        @Size(max = 255, message = "Opis mesta može imati najviše 255 znakova")
        String placeDescription,

        @DecimalMin(value = "-90.0", message = "Neispravna geografska širina")
        @DecimalMax(value = "90.0", message = "Neispravna geografska širina")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Neispravna geografska dužina")
        @DecimalMax(value = "180.0", message = "Neispravna geografska dužina")
        BigDecimal longitude,

        @Min(value = 0, message = "Radijus ne može biti negativan")
        @Max(value = 50000, message = "Radijus može biti najviše 50 km")
        Integer locationRadiusMeters,

        // Detalj VLASNIKA CUVA se šifrovan
        @Size(max = 1000, message = "Privatni detalji mogu imati najviše 1000 znakova")
        String privateDetails,

        @NotEmpty(message = "Oglas mora imati bar jedan predmet")
        @Size(max = 10, message = "Oglas može imati najviše 10 predmeta")
        @Valid
        List<LostReportItemRequest> items
) {}