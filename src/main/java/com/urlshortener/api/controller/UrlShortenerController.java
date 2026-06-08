package com.urlshortener.api.controller;
import com.urlshortener.api.dto.ShortenUrlRequest;
import com.urlshortener.api.dto.ShortenUrlResponse;
import com.urlshortener.api.dto.UserLinksResponse;
import com.urlshortener.api.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Shortener API", description = "Endpoints for link shortening and high-performance redirection")
public class UrlShortenerController {
    private final UrlShortenerService service;

    /*
     * ENDPOINT 1: POST /api/shorten
     * Recebe a URL longa e os dados do usuário, valida as regras de negócio
     * e salva simultaneamente no Redis (com 10 minutos de TTL) e no MongoDB.
     */

    @PostMapping("/api/shorten")
    @Operation(summary = "Create a shortened URL", description = "Enforces a strict 5-link limit per user email and a 10-minute lifetime expiration.")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(@RequestBody @Valid ShortenUrlRequest request) {
        ShortenUrlResponse response = service.shortenUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     * ENDPOINT 2: GET /r/{shortCode}
     * Captura o código de 6 caracteres direto da URL, faz uma busca ultra rápida em memória
     * e redireciona o navegador do usuário com o status HTTP 302 (Found).
     */

    @GetMapping("/r/{shortCode}")
    @Operation(summary = "Redirect to original URL", description = "Performs an ultra-fast RAM memory lookup. Returns custom JSON error if expired.")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = service.getOriginalUrl(shortCode);

        // Retorna o status HTTP 302 com o cabeçalho 'Location' apontando para o site de destino
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    /*
     * ENDPOINT 3: GET /api/urls
     * Retorna o relatório de links cadastrados de forma paginada e agrupada por usuário.
     * Padrão: página 0, trazendo 10 registros por vez.
     */
    @GetMapping("/api/urls")
    @Operation(summary = "Get all shortened URLs with pagination", description = "Returns a clean report of links grouped by user with high data safety.")
    public ResponseEntity<Page<UserLinksResponse>> getAllUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserLinksResponse> response = service.getAllUrlsPaginaded(pageable);
        return ResponseEntity.ok(response);
    }

    /*
     * ENDPOINT 4: DELETE /api/urls/{id}
     * Remove o registro do MongoDB, expulsa o link da memória do Redis
     * e abate o contador de uso simultâneo do usuário.
     */
    @DeleteMapping("/api/urls/{id}")
    @Operation(summary = "Delete link mapping by ID", description = "Deletes historical documents, purges Redis active cache, and releases user quotas.")
    public ResponseEntity<Void> deleteUrl(@PathVariable String id) {
        service.deleteUrlById(id);
        return ResponseEntity.noContent().build(); // Retorna o status HTTP 204 (No Content) padrão de deleções de sucesso
    }
}
