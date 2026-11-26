package br.com.fabricio.url_shortener_api.url;

import java.time.LocalDateTime;

public record ShortUrlResponse(
    Long id,
    String originalUrl,
    String shortCode,
    long clickCount,
    LocalDateTime expiresAt,
    LocalDateTime createdAt) {
}
