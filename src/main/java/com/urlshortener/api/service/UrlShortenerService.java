package com.urlshortener.api.service;

import com.urlshortener.api.dto.ShortenUrlRequest;
import com.urlshortener.api.dto.ShortenUrlResponse;
import com.urlshortener.api.dto.UserLinksResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;




public interface UrlShortenerService {
    ShortenUrlResponse shortenUrl(ShortenUrlRequest request);

    String getOriginalUrl(String shortCode);

    Page<UserLinksResponse> getAllUrlsPaginaded(Pageable pageable);

    void deleteUrlById(String id); // Novo comando
}
