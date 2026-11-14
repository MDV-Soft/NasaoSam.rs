package org.example.nasaosam.found.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinderAccessRequest (
        @NotBlank(message = "token je obavezan")
        @Size(max = 100, message = "token je predugacak")
        String token
){
}
