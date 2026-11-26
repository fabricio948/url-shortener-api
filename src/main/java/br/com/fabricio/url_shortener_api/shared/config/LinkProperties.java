package br.com.fabricio.url_shortener_api.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LinkProperties {

  private final long ttlSeconds;
  private final int maxPerUser;

  public LinkProperties(
      @Value("${app.links.ttl-seconds}") long ttlSeconds,
      @Value("${app.links.max-per-user}") int maxPerUser) {
    this.ttlSeconds = ttlSeconds;
    this.maxPerUser = maxPerUser;
  }

  public long ttlSeconds() {
    return ttlSeconds;
  }

  public int maxPerUser() {
    return maxPerUser;
  }
}
