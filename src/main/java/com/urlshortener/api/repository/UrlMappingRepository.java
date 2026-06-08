package com.urlshortener.api.repository;

import com.urlshortener.api.domain.UrlMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;



@Repository
public interface UrlMappingRepository  extends MongoRepository<UrlMapping, String> {
    Optional<UrlMapping> findByShortCode(String shortCode);

    long countByUserEmail(String userEmail);
}
