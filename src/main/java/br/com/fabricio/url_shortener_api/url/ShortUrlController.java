package br.com.fabricio.url_shortener_api.url;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {

  private final ShortUrlService shortUrlService;

  public ShortUrlController(ShortUrlService shortUrlService) {
    this.shortUrlService = shortUrlService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ShortUrlResponse create(
      @Valid @RequestBody CreateShortUrlRequest request,
      @AuthenticationPrincipal Long userId) {
    return shortUrlService.create(request, userId);
  }

  @GetMapping
  public List<ShortUrlResponse> list(@AuthenticationPrincipal Long userId) {
    return shortUrlService.list(userId);
  }

  @GetMapping("/{shortCode}/stats")
  public ShortUrlStatsResponse stats(
      @PathVariable String shortCode, @AuthenticationPrincipal Long userId) {
    return shortUrlService.getStats(userId, shortCode);
  }

  @DeleteMapping("/{shortCode}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable String shortCode, @AuthenticationPrincipal Long userId) {
    shortUrlService.delete(userId, shortCode);
  }
}
