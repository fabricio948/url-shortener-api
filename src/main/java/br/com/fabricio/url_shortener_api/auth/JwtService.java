package br.com.fabricio.url_shortener_api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final Algorithm algorithm;
  private final long expirationSeconds;
  private final Clock clock;

  public JwtService(
      @Value("${JWT_SECRET}") String secret,
      @Value("${app.jwt.expiration-seconds}") long expirationSeconds,
      Clock clock) {
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException("JWT_SECRET deve ter no mínimo 32 caracteres.");
    }
    this.algorithm = Algorithm.HMAC256(secret);
    this.expirationSeconds = expirationSeconds;
    this.clock = clock;
  }

  public String generateToken(Long userId) {
    Instant now = clock.instant();
    return JWT.create()
        .withSubject(userId.toString())
        .withExpiresAt(now.plusSeconds(expirationSeconds))
        .sign(algorithm);
  }

  public long getExpirationSeconds() {
    return expirationSeconds;
  }

  public Long validateTokenAndGetUserId(String token) {
    try {
      DecodedJWT decoded = JWT.decode(token);
      algorithm.verify(decoded);

      Instant expiresAt = decoded.getExpiresAtAsInstant();
      if (expiresAt == null || !expiresAt.isAfter(clock.instant())) {
        throw new JWTVerificationException("Token expirado.");
      }

      return Long.valueOf(decoded.getSubject());
    } catch (NumberFormatException exception) {
      throw new JWTVerificationException("Token inválido.", exception);
    }
  }
}
