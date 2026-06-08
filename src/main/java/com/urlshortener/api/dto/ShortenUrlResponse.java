package com.urlshortener.api.dto;

import java.time.Instant;

public record ShortenUrlResponse(
        String shortUrl,
        String originalUrl,
        Instant expiresAt
) {
}
