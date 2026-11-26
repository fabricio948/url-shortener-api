package br.com.fabricio.url_shortener_api.url;

import br.com.fabricio.url_shortener_api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "short_urls",
    indexes = @Index(name = "idx_short_code", columnList = "short_code", unique = true))
public class ShortUrl {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "original_url", nullable = false, length = 2048)
  private String originalUrl;

  @Column(name = "short_code", nullable = false, length = 20)
  private String shortCode;

  @Column(name = "click_count", nullable = false)
  private long clickCount;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  protected ShortUrl() {
  }

  public ShortUrl(
      String originalUrl,
      String shortCode,
      long clickCount,
      LocalDateTime expiresAt,
      LocalDateTime createdAt,
      User user) {
    this.originalUrl = originalUrl;
    this.shortCode = shortCode;
    this.clickCount = clickCount;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
    this.user = user;
  }

  public Long getId() {
    return id;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public String getShortCode() {
    return shortCode;
  }

  public long getClickCount() {
    return clickCount;
  }

  public void setClickCount(long clickCount) {
    this.clickCount = clickCount;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public User getUser() {
    return user;
  }
}
