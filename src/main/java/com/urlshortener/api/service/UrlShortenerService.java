package com.urlshortener.api.service;

import com.urlshortener.api.dto.ShortenUrlRequest;
import com.urlshortener.api.dto.ShortenUrlResponse;

public interface UrlShortenerService {
    ShortenUrlResponse shortenUrl(ShortenUrlRequest request);

    String getOriginalUrl(String shortCode);
}
