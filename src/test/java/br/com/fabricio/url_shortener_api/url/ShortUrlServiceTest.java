package br.com.fabricio.url_shortener_api.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fabricio.url_shortener_api.shared.config.LinkProperties;
import br.com.fabricio.url_shortener_api.shared.exception.ActiveLinkLimitReachedException;
import br.com.fabricio.url_shortener_api.shared.exception.ResourceAccessDeniedException;
import br.com.fabricio.url_shortener_api.shared.exception.ResourceNotFoundException;
import br.com.fabricio.url_shortener_api.shared.exception.ShortCodeAlreadyExistsException;
import br.com.fabricio.url_shortener_api.user.User;
import br.com.fabricio.url_shortener_api.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

  private static final Long USER_ID = 1L;
  private static final Instant NOW = Instant.parse("2026-11-26T14:30:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final LocalDateTime NOW_DATE_TIME =
      LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
  private static final long TTL_SECONDS = 300;
  private static final int MAX_PER_USER = 5;

  @Mock private ShortUrlRepository shortUrlRepository;
  @Mock private UserRepository userRepository;
  @Mock private ShortCodeGenerator shortCodeGenerator;

  private ShortUrlService shortUrlService;
  private User user;

  @BeforeEach
  void setUp() {
    LinkProperties linkProperties = new LinkProperties(TTL_SECONDS, MAX_PER_USER);
    shortUrlService =
        new ShortUrlService(
            shortUrlRepository, userRepository, shortCodeGenerator, CLOCK, linkProperties);
    user = mock(User.class);
  }

  @Test
  void shouldCreateShortUrlWithGeneratedCodeWhenAliasIsNotProvided() {
    // Dado
    CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com/pagina", null);
    when(shortUrlRepository.countByUserIdAndExpiresAtAfter(USER_ID, NOW_DATE_TIME))
        .thenReturn(0L);
    when(shortCodeGenerator.generate()).thenReturn("aB3x9Kz");
    when(shortUrlRepository.existsByShortCode("aB3x9Kz")).thenReturn(false);
    when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
    when(shortUrlRepository.save(any(ShortUrl.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Quando
    ShortUrlResponse response = shortUrlService.create(request, USER_ID);

    // Então
    assertThat(response.originalUrl()).isEqualTo("https://example.com/pagina");
    assertThat(response.shortCode()).isEqualTo("aB3x9Kz");
    assertThat(response.clickCount()).isZero();
    assertThat(response.expiresAt()).isEqualTo(NOW_DATE_TIME.plusSeconds(TTL_SECONDS));
    assertThat(response.createdAt()).isEqualTo(NOW_DATE_TIME);

    ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
    verify(shortUrlRepository).save(captor.capture());
    assertThat(captor.getValue().getUser()).isSameAs(user);
  }

  @Test
  void shouldUseCustomAliasWhenProvided() {
    // Dado
    CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com", "meuAlias");
    when(shortUrlRepository.countByUserIdAndExpiresAtAfter(USER_ID, NOW_DATE_TIME))
        .thenReturn(0L);
    when(shortUrlRepository.existsByShortCode("meuAlias")).thenReturn(false);
    when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
    when(shortUrlRepository.save(any(ShortUrl.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Quando
    ShortUrlResponse response = shortUrlService.create(request, USER_ID);

    // Então
    assertThat(response.shortCode()).isEqualTo("meuAlias");
    verifyNoInteractions(shortCodeGenerator);
  }

  @Test
  void shouldThrowWhenCustomAliasAlreadyExists() {
    // Dado
    CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com", "meuAlias");
    when(shortUrlRepository.countByUserIdAndExpiresAtAfter(USER_ID, NOW_DATE_TIME))
        .thenReturn(0L);
    when(shortUrlRepository.existsByShortCode("meuAlias")).thenReturn(true);

    // Quando / Então
    assertThatThrownBy(() -> shortUrlService.create(request, USER_ID))
        .isInstanceOf(ShortCodeAlreadyExistsException.class)
        .hasMessage("Código curto já existe.");
    verify(shortUrlRepository, never()).save(any(ShortUrl.class));
    verifyNoInteractions(shortCodeGenerator);
  }

  @Test
  void shouldThrowWhenActiveLinkLimitIsReached() {
    // Dado
    CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com", null);
    when(shortUrlRepository.countByUserIdAndExpiresAtAfter(USER_ID, NOW_DATE_TIME))
        .thenReturn((long) MAX_PER_USER);

    // Quando / Então
    assertThatThrownBy(() -> shortUrlService.create(request, USER_ID))
        .isInstanceOf(ActiveLinkLimitReachedException.class)
        .hasMessage("Limite de 5 links ativos atingido. Exclua um link ou aguarde um expirar.");
    verify(shortUrlRepository, never()).save(any(ShortUrl.class));
    verifyNoInteractions(shortCodeGenerator);
  }

  @Test
  void shouldCreateWhenActiveLinksAreBelowLimit() {
    // Dado: a contagem considera só links cujo expiresAt está depois de agora
    CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com", null);
    when(shortUrlRepository.countByUserIdAndExpiresAtAfter(USER_ID, NOW_DATE_TIME))
        .thenReturn((long) MAX_PER_USER - 1);
    when(shortCodeGenerator.generate()).thenReturn("aB3x9Kz");
    when(shortUrlRepository.existsByShortCode("aB3x9Kz")).thenReturn(false);
    when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
    when(shortUrlRepository.save(any(ShortUrl.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Quando
    ShortUrlResponse response = shortUrlService.create(request, USER_ID);

    // Então
    assertThat(response.shortCode()).isEqualTo("aB3x9Kz");
    verify(shortUrlRepository).countByUserIdAndExpiresAtAfter(USER_ID, NOW_DATE_TIME);
  }

  @Test
  void shouldThrowWhenGeneratedCodeKeepsColliding() {
    // Dado
    CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com", null);
    when(shortUrlRepository.countByUserIdAndExpiresAtAfter(USER_ID, NOW_DATE_TIME))
        .thenReturn(0L);
    when(shortCodeGenerator.generate()).thenReturn("aB3x9Kz");
    when(shortUrlRepository.existsByShortCode("aB3x9Kz")).thenReturn(true);

    // Quando / Então
    assertThatThrownBy(() -> shortUrlService.create(request, USER_ID))
        .isInstanceOf(ShortCodeAlreadyExistsException.class)
        .hasMessage("Código curto já existe.");
    verify(shortCodeGenerator, times(5)).generate();
    verify(shortUrlRepository, never()).save(any(ShortUrl.class));
  }

  @Test
  void shouldListOnlyUserLinksNewestFirst() {
    // Dado
    ShortUrl newer = shortUrl("novo", NOW_DATE_TIME.minusMinutes(1));
    ShortUrl older = shortUrl("antigo", NOW_DATE_TIME.minusMinutes(5));
    when(shortUrlRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
        .thenReturn(List.of(newer, older));

    // Quando
    List<ShortUrlResponse> responses = shortUrlService.list(USER_ID);

    // Então
    assertThat(responses)
        .extracting(ShortUrlResponse::shortCode)
        .containsExactly("novo", "antigo");
    verify(shortUrlRepository).findByUserIdOrderByCreatedAtDesc(USER_ID);
  }

  @Test
  void shouldReturnEmptyListWhenUserHasNoLinks() {
    // Dado
    when(shortUrlRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());

    // Quando / Então
    assertThat(shortUrlService.list(USER_ID)).isEmpty();
  }

  @Test
  void shouldDeleteOwnLink() {
    // Dado
    ShortUrl shortUrl = shortUrl("meuLink", NOW_DATE_TIME);
    when(shortUrlRepository.findByShortCode("meuLink")).thenReturn(Optional.of(shortUrl));
    when(user.getId()).thenReturn(USER_ID);

    // Quando
    shortUrlService.delete(USER_ID, "meuLink");

    // Então
    verify(shortUrlRepository).delete(shortUrl);
  }

  @Test
  void shouldThrowWhenDeletingUnknownLink() {
    // Dado
    when(shortUrlRepository.findByShortCode("naoexiste")).thenReturn(Optional.empty());

    // Quando / Então
    assertThatThrownBy(() -> shortUrlService.delete(USER_ID, "naoexiste"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Link não encontrado.");
  }

  @Test
  void shouldThrowWhenDeletingLinkFromAnotherUser() {
    // Dado
    ShortUrl shortUrl = shortUrl("meuLink", NOW_DATE_TIME);
    when(shortUrlRepository.findByShortCode("meuLink")).thenReturn(Optional.of(shortUrl));
    when(user.getId()).thenReturn(2L);

    // Quando / Então
    assertThatThrownBy(() -> shortUrlService.delete(USER_ID, "meuLink"))
        .isInstanceOf(ResourceAccessDeniedException.class)
        .hasMessage("Você não tem permissão para acessar este link.");
    verify(shortUrlRepository, never()).delete(any(ShortUrl.class));
  }

  private ShortUrl shortUrl(String shortCode, LocalDateTime createdAt) {
    return new ShortUrl(
        "https://example.com",
        shortCode,
        0L,
        createdAt.plusSeconds(TTL_SECONDS),
        createdAt,
        user);
  }
}
