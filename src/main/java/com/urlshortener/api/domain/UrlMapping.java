package com.urlshortener.api.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "url_mappings")
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class UrlMapping {

    @Id
    private String id;

    @Indexed(unique = true)
    private String shortCode;

    private String originalUrl;
    private String userName;

    @Indexed
    private String userEmail;

    private Instant createdAt;
    private Instant expiresAt;

}
