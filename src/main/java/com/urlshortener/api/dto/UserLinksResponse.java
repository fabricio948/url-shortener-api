package com.urlshortener.api.dto;

import java.util.List;

public record UserLinksResponse(
        String userName,
        String userEmail,
        List<LinkInfo> links
) {
    public record LinkInfo(
            String id, // O ID do MongoDB adicionado aqui para permitir a deleção
            String shortUrl,
            String originalUrl,
            String shortCode
    ) {}
}
