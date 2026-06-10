package com.urlshortener.api.service.impl;

import com.urlshortener.api.domain.UrlMapping;
import com.urlshortener.api.dto.ShortenUrlRequest;
import com.urlshortener.api.dto.ShortenUrlResponse;
import com.urlshortener.api.dto.UserLinksResponse;
import com.urlshortener.api.repository.UrlMappingRepository;
import com.urlshortener.api.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        // REGRA NOVA: Alterado rigorosamente para o limite de 4 links ativos
        if (currentCount >= 3) {
            log.warn("[LIMIT BLOCKED] User {} blocked. Current active links: {}", request.userEmail(), currentCount);
            throw new IllegalStateException("User has reached the strict limit of 3 active links.");        }

        String shortCode = generateShortCode(request.originalUrl(), request.userEmail());
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(10));

        log.info("[MONGODB] Persisting historical mapping in database. Code: {} -> URL: {}", shortCode, request.originalUrl());
        UrlMapping urlMapping = new UrlMapping(null, shortCode, request.originalUrl(), request.userName(), request.userEmail(), now, expiresAt);
        repository.save(urlMapping);

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

    @Override
    public Page<UserLinksResponse> getAllUrlsPaginaded(Pageable pageable) {
        log.info("[MONGODB] Fetching page {} from database for unified report.", pageable.getPageNumber());

        // Puxa do MongoDB usando paginação nativa para não estourar a memória RAM
        Page<UrlMapping> entityPage = repository.findAll(pageable);

        // Agrupa os links por E-mail do usuário usando a Stream API de forma organizada
        Map<String, List<UrlMapping>> groupedByUser = entityPage.getContent().stream()
                .collect(Collectors.groupingBy(UrlMapping::getUserEmail));

        // Mapeia o resultado agrupado para o nosso DTO estruturado
        return entityPage.map(mapping -> {
            List<UrlMapping> userMappings = groupedByUser.get(mapping.getUserEmail());

            List<UserLinksResponse.LinkInfo> linkInfos = userMappings.stream()
                    .map(m -> new UserLinksResponse.LinkInfo(
                            m.getId(), // INJEÇÃO DO ID DO MONGODB AQUI
                            "http://localhost:8080/r/" + m.getShortCode(),
                            m.getOriginalUrl(),
                            m.getShortCode()
                    ))
                    .collect(Collectors.toList());

            return new UserLinksResponse(mapping.getUserName(), mapping.getUserEmail(), linkInfos);
        });
    }

    @Override
    public void deleteUrlById(String id) {
        log.info("[MONGODB] Searching link mapping for deletion. ID: {}", id);
        UrlMapping urlMapping = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Link mapping not found for the provided ID."));

        // 1. Apagar do MongoDB (Histórico)
        log.info("[MONGODB] Deleting historical record ID: {}", id);
        repository.deleteById(id);

        // 2. Apagar da memória ativa do Redis
        String redisUrlKey = URL_PREFIX + urlMapping.getShortCode();
        log.info("[REDIS] Evicting active cache short link key: {}", redisUrlKey);
        redisTemplate.delete(redisUrlKey);

        // 3. Decrementar o limite do usuário para liberar espaço para novos encurtamentos
        String userLimitKey = USER_LIMIT_PREFIX + urlMapping.getUserEmail();
        String currentCountStr = redisTemplate.opsForValue().get(userLimitKey);
        if (currentCountStr != null && Integer.parseInt(currentCountStr) > 0) {
            log.info("[REDIS] Decrementing link usage count for user: {}", urlMapping.getUserEmail());
            redisTemplate.opsForValue().decrement(userLimitKey);
        }
        log.info("[SUCCESS] Record completely deleted from infrastructure.");
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
