package br.com.fabricio.url_shortener_api.auth;

import br.com.fabricio.url_shortener_api.shared.exception.EmailAlreadyExistsException;
import br.com.fabricio.url_shortener_api.shared.exception.InvalidCredentialsException;
import br.com.fabricio.url_shortener_api.user.User;
import br.com.fabricio.url_shortener_api.user.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final Clock clock;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.clock = clock;
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);

    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException();
    }

    User user =
        new User(
            request.name(),
            email,
            passwordEncoder.encode(request.password()),
            LocalDateTime.now(clock));

    try {
      User savedUser = userRepository.saveAndFlush(user);
      return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    } catch (DataIntegrityViolationException exception) {
      throw new EmailAlreadyExistsException();
    }
  }

  @Transactional
  public LoginResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new InvalidCredentialsException();
    }

    String token = jwtService.generateToken(user.getId());
    return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds());
  }
}
