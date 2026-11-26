package br.com.fabricio.url_shortener_api.url;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

  Optional<ShortUrl> findByShortCode(String shortCode);

  boolean existsByShortCode(String shortCode);

  long countByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now);

  List<ShortUrl> findByUserIdOrderByCreatedAtDesc(Long userId);
}
