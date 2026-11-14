package org.example.nasaosam.found.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.nasaosam.found.entity.ItemLocation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Zahtev nalazaca
 */
public record CreateFoundReportRequest(

        @NotBlank(message = "Mejl je obavezan")
        @Email(message = "Unesi ispravnu mejl adresu")
        @Size(max = 255, message = "Mejl može imati najviše 255 znakova")
        String finderEmail,

        // Oblik broja (srpski mobilni) proverava PhoneNumberNormalizer u servisu
        @Size(max = 30, message = "Broj telefona može imati najviše 30 znakova")
        String finderPhone,

        @NotNull(message = "Vreme nalaska je obavezno")
        Instant foundAt,

        @NotBlank(message = "Grad je obavezan")
        @Size(max = 100, message = "Grad može imati najviše 100 znakova")
        String city,

        @Size(max = 255, message = "Opis mesta može imati najviše 255 znakova")
        String placeDescription,

        // Tačka na mapi je obavezna: bez nje spajanje ne može da meri udaljenost
        @NotNull(message = "Tačka na mapi je obavezna")
        @DecimalMin(value = "-90.0", message = "Neispravna geografska širina")
        @DecimalMax(value = "90.0", message = "Neispravna geografska širina")
        BigDecimal latitude,

        @NotNull(message = "Tačka na mapi je obavezna")
        @DecimalMin(value = "-180.0", message = "Neispravna geografska dužina")
        @DecimalMax(value = "180.0", message = "Neispravna geografska dužina")
        BigDecimal longitude,

        @NotNull(message = "Navedi gde se predmet nalazi")
        ItemLocation itemLocation,

        @Size(max = 255, message = "Podaci o predaji mogu imati najviše 255 znakova")
        String handoverInfo,

        @NotBlank(message = "Kontrolno pitanje je obavezno")
        @Size(max = 255, message = "Pitanje može imati najviše 255 znakova")
        String controlQuestion,

        @NotBlank(message = "Odgovor na kontrolno pitanje je obavezan")
        @Size(max = 500, message = "Odgovor može imati najviše 500 znakova")
        String controlAnswer,

        @NotEmpty(message = "Nalaz mora imati bar jedan predmet")
        @Size(max = 10, message = "Nalaz može imati najviše 10 predmeta")
        @Valid
        List<FoundReportItemRequest> items
) {
}