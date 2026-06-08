package com.urlshortener.api.service.impl;

import com.urlshortener.api.domain.UrlMapping;
import com.urlshortener.api.dto.ShortenUrlRequest;
import com.urlshortener.api.dto.ShortenUrlResponse;
import com.urlshortener.api.repository.UrlMappingRepository;
import com.urlshortener.api.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private final UrlMappingRepository repository;
    private final StringRedisTemplate redisTemplate;

    private static final String USER_LIMIT_PREFIX = "limit:";
    private static final String URL_PREFIX = "url:";

    @Override
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {
        String userLimitKey = USER_LIMIT_PREFIX + request.userEmail();

        log.info("[REDIS] Checking link creation limit for user email: {}", request.userEmail());
        String currentCountStr = redisTemplate.opsForValue().get(userLimitKey);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

        if (currentCount >= 5) {
            log.warn("[LIMIT BLOCKED] User {} blocked. Current active links: {}", request.userEmail(), currentCount);
            throw new IllegalStateException("User has reached the limit of 5 active links.");
        }

        String shortCode = generateShortCode(request.originalUrl(), request.userEmail());
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(10));

        // Logs para o MongoDB
        log.info("[MONGODB] Persisting historical mapping in database. Code: {} -> URL: {}", shortCode, request.originalUrl());
        UrlMapping urlMapping = new UrlMapping(null, shortCode, request.originalUrl(), request.userName(), request.userEmail(), now, expiresAt);
        repository.save(urlMapping);

        // Logs para o Redis
        log.info("[REDIS] Saving active short link with 10 minutes TTL. Key: {}{}", URL_PREFIX, shortCode);
        redisTemplate.opsForValue().set(URL_PREFIX + shortCode, request.originalUrl(), Duration.ofMinutes(10));

        log.info("[REDIS] Incrementing simultaneous link counter for user: {}", request.userEmail());
        redisTemplate.opsForValue().increment(userLimitKey);
        if (currentCount == 0) {
            redisTemplate.expire(userLimitKey, Duration.ofMinutes(10));
        }

        log.info("[SUCCESS] URL shortened successfully. Generated code: {}", shortCode);
        return new ShortenUrlResponse("http://localhost:8080/r/" + shortCode, request.originalUrl(), expiresAt);
    }

    @Override
    public String getOriginalUrl(String shortCode) {
        log.info("[REDIS] Attempting high-performance redirect search for code: {}", shortCode);
        String originalUrl = redisTemplate.opsForValue().get(URL_PREFIX + shortCode);

        if (originalUrl == null) {
            log.error("[NOT FOUND/EXPIRED] Code {} not found or already expired in Redis memory.", shortCode);
            throw new IllegalArgumentException("This link has expired or does not exist.");
        }

        log.info("[REDIRECT] URL found in memory. Redirecting user to: {}", originalUrl);
        return originalUrl;
    }

    private String generateShortCode(String url, String email) {
        try {
            String input = url + email + Instant.now().toEpochMilli();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return base64.substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            log.error("[CRYPTO ERROR] Failed to instantiate SHA-256 algorithm");
            throw new RuntimeException("Error generating cryptographic hash", e);
        }
    }
}
