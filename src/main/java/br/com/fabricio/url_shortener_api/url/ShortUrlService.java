package br.com.fabricio.url_shortener_api.url;

import br.com.fabricio.url_shortener_api.shared.config.LinkProperties;
import br.com.fabricio.url_shortener_api.shared.exception.ActiveLinkLimitReachedException;
import br.com.fabricio.url_shortener_api.shared.exception.ResourceAccessDeniedException;
import br.com.fabricio.url_shortener_api.shared.exception.ResourceNotFoundException;
import br.com.fabricio.url_shortener_api.shared.exception.ShortCodeAlreadyExistsException;
import br.com.fabricio.url_shortener_api.user.User;
import br.com.fabricio.url_shortener_api.user.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlService {

  private static final int MAX_GENERATION_ATTEMPTS = 5;

  private final ShortUrlRepository shortUrlRepository;
  private final UserRepository userRepository;
  private final ShortCodeGenerator shortCodeGenerator;
  private final Clock clock;
  private final LinkProperties linkProperties;

  public ShortUrlService(
      ShortUrlRepository shortUrlRepository,
      UserRepository userRepository,
      ShortCodeGenerator shortCodeGenerator,
      Clock clock,
      LinkProperties linkProperties) {
    this.shortUrlRepository = shortUrlRepository;
    this.userRepository = userRepository;
    this.shortCodeGenerator = shortCodeGenerator;
    this.clock = clock;
    this.linkProperties = linkProperties;
  }

  @Transactional
  public ShortUrlResponse create(CreateShortUrlRequest request, Long userId) {
    LocalDateTime now = LocalDateTime.now(clock).withNano(0);
    long activeLinks = shortUrlRepository.countByUserIdAndExpiresAtAfter(userId, now);
    if (activeLinks >= linkProperties.maxPerUser()) {
      throw new ActiveLinkLimitReachedException();
    }

    String shortCode = resolveShortCode(request.customAlias());
    User user = userRepository.getReferenceById(userId);
    ShortUrl shortUrl =
        new ShortUrl(
            request.originalUrl(),
            shortCode,
            0L,
            now.plusSeconds(linkProperties.ttlSeconds()),
            now,
            user);

    ShortUrl savedShortUrl = shortUrlRepository.save(shortUrl);
    return toResponse(savedShortUrl);
  }

  @Transactional(readOnly = true)
  public List<ShortUrlResponse> list(Long userId) {
    return shortUrlRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public void delete(Long userId, String shortCode) {
    ShortUrl shortUrl =
        shortUrlRepository.findByShortCode(shortCode).orElseThrow(ResourceNotFoundException::new);

    if (!shortUrl.getUser().getId().equals(userId)) {
      throw new ResourceAccessDeniedException();
    }

    shortUrlRepository.delete(shortUrl);
  }

  private String resolveShortCode(String customAlias) {
    if (customAlias != null) {
      if (shortUrlRepository.existsByShortCode(customAlias)) {
        throw new ShortCodeAlreadyExistsException();
      }
      return customAlias;
    }

    for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
      String generated = shortCodeGenerator.generate();
      if (!shortUrlRepository.existsByShortCode(generated)) {
        return generated;
      }
    }

    throw new ShortCodeAlreadyExistsException();
  }

  private ShortUrlResponse toResponse(ShortUrl shortUrl) {
    return new ShortUrlResponse(
        shortUrl.getId(),
        shortUrl.getOriginalUrl(),
        shortUrl.getShortCode(),
        shortUrl.getClickCount(),
        shortUrl.getExpiresAt(),
        shortUrl.getCreatedAt());
  }
}
