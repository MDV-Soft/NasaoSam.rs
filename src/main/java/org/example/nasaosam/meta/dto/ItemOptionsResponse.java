package org.example.nasaosam.meta.dto;

import java.util.List;

public record ItemOptionsResponse(
        List<OptionResponse> categories,
        List<OptionResponse> documentTypes,
        List<OptionResponse> colors
) {
}