package br.com.fabricio.url_shortener_api.url;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class RandomShortCodeGenerator implements ShortCodeGenerator {

  private static final String ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final int CODE_LENGTH = 7;

  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public String generate() {
    StringBuilder code = new StringBuilder(CODE_LENGTH);
    for (int index = 0; index < CODE_LENGTH; index++) {
      code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }
}
