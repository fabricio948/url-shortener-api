package br.com.fabricio.url_shortener_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fabricio.url_shortener_api.shared.exception.EmailAlreadyExistsException;
import br.com.fabricio.url_shortener_api.shared.exception.InvalidCredentialsException;
import br.com.fabricio.url_shortener_api.user.User;
import br.com.fabricio.url_shortener_api.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-26T14:30:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  private PasswordEncoder passwordEncoder;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    authService = new AuthService(userRepository, passwordEncoder, jwtService, CLOCK);
  }

  @Test
  void shouldRegisterUserAndReturnResponseWithoutPassword() {
    // Dado
    RegisterRequest request =
        new RegisterRequest("Fabricio", "fabricio@example.com", "senha1234");
    when(userRepository.existsByEmail("fabricio@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Quando
    UserResponse response = authService.register(request);

    // Então
    assertThat(response.name()).isEqualTo("Fabricio");
    assertThat(response.email()).isEqualTo("fabricio@example.com");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).saveAndFlush(captor.capture());
    User savedUser = captor.getValue();
    assertThat(savedUser.getPassword()).isNotEqualTo("senha1234");
    assertThat(passwordEncoder.matches("senha1234", savedUser.getPassword())).isTrue();
    assertThat(savedUser.getCreatedAt())
        .isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
  }

  @Test
  void shouldNormalizeEmailBeforeCheckingAndSaving() {
    // Dado
    RegisterRequest request =
        new RegisterRequest("Fabricio", "  Fabricio@Example.COM  ", "senha1234");
    when(userRepository.existsByEmail("fabricio@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Quando
    authService.register(request);

    // Então
    verify(userRepository).existsByEmail("fabricio@example.com");
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("fabricio@example.com");
  }

  @Test
  void shouldThrowWhenEmailAlreadyExists() {
    // Dado
    RegisterRequest request =
        new RegisterRequest("Fabricio", "fabricio@example.com", "senha1234");
    when(userRepository.existsByEmail("fabricio@example.com")).thenReturn(true);

    // Quando / Então
    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessage("E-mail já cadastrado.");
    verify(userRepository, never()).saveAndFlush(any(User.class));
  }

  @Test
  void shouldThrowWhenUniqueConstraintIsViolated() {
    // Dado
    RegisterRequest request =
        new RegisterRequest("Fabricio", "fabricio@example.com", "senha1234");
    when(userRepository.existsByEmail("fabricio@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("unique constraint"));

    // Quando / Então
    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessage("E-mail já cadastrado.");
  }

  @Test
  void shouldLoginAndReturnToken() {
    // Dado
    User user = existingUser("senha1234");
    when(userRepository.findByEmail("fabricio@example.com")).thenReturn(Optional.of(user));
    when(jwtService.generateToken(any())).thenReturn("jwt-token");
    when(jwtService.getExpirationSeconds()).thenReturn(300L);

    // Quando
    LoginResponse response =
        authService.login(new LoginRequest("fabricio@example.com", "senha1234"));

    // Então
    assertThat(response.token()).isEqualTo("jwt-token");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(300);
  }

  @Test
  void shouldThrowWhenPasswordIsWrong() {
    // Dado
    User user = existingUser("senha1234");
    when(userRepository.findByEmail("fabricio@example.com")).thenReturn(Optional.of(user));

    // Quando / Então
    assertThatThrownBy(
            () -> authService.login(new LoginRequest("fabricio@example.com", "senha-errada")))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Credenciais inválidas ou token ausente.");
    verifyNoInteractions(jwtService);
  }

  @Test
  void shouldThrowWhenEmailDoesNotExist() {
    // Dado
    when(userRepository.findByEmail("naoexiste@example.com")).thenReturn(Optional.empty());

    // Quando / Então
    assertThatThrownBy(
            () -> authService.login(new LoginRequest("naoexiste@example.com", "senha1234")))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Credenciais inválidas ou token ausente.");
    verifyNoInteractions(jwtService);
  }

  private User existingUser(String rawPassword) {
    return new User(
        "Fabricio",
        "fabricio@example.com",
        passwordEncoder.encode(rawPassword),
        LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
  }
}
