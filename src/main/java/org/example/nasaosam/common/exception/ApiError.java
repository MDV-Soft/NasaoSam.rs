package org.example.nasaosam.common.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
}
