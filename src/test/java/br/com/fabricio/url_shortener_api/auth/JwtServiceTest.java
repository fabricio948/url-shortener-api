package br.com.fabricio.url_shortener_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "test-secret-with-at-least-32-characters-long";
  private static final long EXPIRATION_SECONDS = 300;
  private static final Instant NOW = Instant.parse("2026-11-26T14:30:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(SECRET, EXPIRATION_SECONDS, CLOCK);
  }

  @Test
  void shouldGenerateTokenWithUserIdAsSubjectAndConfiguredExpiration() {
    // Dado
    Long userId = 42L;

    // Quando
    String token = jwtService.generateToken(userId);

    // Então
    DecodedJWT decoded = JWT.decode(token);
    assertThat(decoded.getSubject()).isEqualTo("42");
    assertThat(decoded.getExpiresAtAsInstant())
        .isEqualTo(NOW.plusSeconds(EXPIRATION_SECONDS));
    assertThat(jwtService.getExpirationSeconds()).isEqualTo(EXPIRATION_SECONDS);
  }

  @Test
  void shouldValidateTokenAndReturnUserId() {
    // Dado
    String token = jwtService.generateToken(42L);

    // Quando
    Long userId = jwtService.validateTokenAndGetUserId(token);

    // Então
    assertThat(userId).isEqualTo(42L);
  }

  @Test
  void shouldRejectExpiredToken() {
    // Dado
    String token = jwtService.generateToken(42L);
    Clock afterExpiration = Clock.fixed(NOW.plusSeconds(EXPIRATION_SECONDS + 1), ZoneOffset.UTC);
    JwtService laterJwtService = new JwtService(SECRET, EXPIRATION_SECONDS, afterExpiration);

    // Quando / Então
    assertThatThrownBy(() -> laterJwtService.validateTokenAndGetUserId(token))
        .isInstanceOf(JWTVerificationException.class);
  }

  @Test
  void shouldRejectTokenSignedWithAnotherSecret() {
    // Dado
    String token = jwtService.generateToken(42L);
    JwtService otherJwtService =
        new JwtService("another-secret-with-at-least-32-characters-123", EXPIRATION_SECONDS, CLOCK);

    // Quando / Então
    assertThatThrownBy(() -> otherJwtService.validateTokenAndGetUserId(token))
        .isInstanceOf(SignatureVerificationException.class);
  }

  @Test
  void shouldFailWhenSecretIsShorterThan32Characters() {
    // Quando / Então
    assertThatThrownBy(() -> new JwtService("curto", EXPIRATION_SECONDS, CLOCK))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("JWT_SECRET deve ter no mínimo 32 caracteres.");
  }
}
