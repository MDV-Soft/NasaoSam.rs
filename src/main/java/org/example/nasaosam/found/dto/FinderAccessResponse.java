package org.example.nasaosam.found.dto;

import java.time.Instant;

public record FinderAccessResponse (
        String accessToken,
        Instant expiresAt
){
}
