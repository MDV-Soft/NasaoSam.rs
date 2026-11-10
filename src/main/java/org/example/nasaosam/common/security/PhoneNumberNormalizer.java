package org.example.nasaosam.common.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PhoneNumberNormalizer {
    private static final String SERBIA_PREFIX = "+381";
    private static final String VALID_PATTERN = "\\+3816\\d{7,8}";
    public String normalize(String rawPhone){
        String cleaned;

        if (rawPhone == null) {
            throw invalidPhone();
        }

        cleaned = rawPhone.trim().replaceAll("[\\s\\-./()]", "");

        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }

        if (cleaned.startsWith("381")) {
            cleaned = "+" + cleaned;
        }

        if (cleaned.startsWith("0")) {
            cleaned = SERBIA_PREFIX + cleaned.substring(1);
        }

        if (!cleaned.matches(VALID_PATTERN)) {
            throw invalidPhone();
        }
        return cleaned;
    }

    private ResponseStatusException invalidPhone() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unesi ispravan srpski mobilni broj, npr. 064 123 4567");
    }
}
