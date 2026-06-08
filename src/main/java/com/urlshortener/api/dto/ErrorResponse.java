package com.urlshortener.api.dto;

import java.time.Instant;

public record ErrorResponse(
        String message,
        int statusCode,
        Instant timestamp
) {
}
