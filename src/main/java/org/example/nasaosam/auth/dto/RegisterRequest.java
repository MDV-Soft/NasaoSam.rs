package org.example.nasaosam.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest (
        @NotBlank(message = "Mejl je obavezan")
        @Email(message = "Unesi ispravnu mejl adresu")
        @Size(max = 255, message = "Mejl može imati najviše 255 znakova")
        String email,

        @NotBlank(message = "Lozinka je obavezna")
        @Size(min = 8, max = 72, message = "Lozinka mora imati od 8 do 72 znaka")
        String password,

        @Size(max = 100, message = "Ime može imati najviše 100 znakova")
        String displayName,

        @NotBlank(message = "Broj telefona je obavezan")
        @Size(max = 30, message = "Broj telefona može imati najviše 30 znakova")
        String phone
){}
