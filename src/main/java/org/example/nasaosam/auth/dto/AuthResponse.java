package org.example.nasaosam.auth.dto;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String role
) {
}