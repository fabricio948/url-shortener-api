package br.com.fabricio.url_shortener_api.url;

import java.time.LocalDateTime;

public record ShortUrlStatsResponse(
    String shortCode,
    String originalUrl,
    long clickCount,
    LocalDateTime createdAt,
    LocalDateTime expiresAt) {
}
