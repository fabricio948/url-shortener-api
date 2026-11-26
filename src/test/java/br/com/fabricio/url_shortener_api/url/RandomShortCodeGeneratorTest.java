package br.com.fabricio.url_shortener_api.url;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RandomShortCodeGeneratorTest {

  private final RandomShortCodeGenerator generator = new RandomShortCodeGenerator();

  @Test
  void shouldGenerateSevenCharacterBase62Code() {
    // Quando / Então
    for (int index = 0; index < 100; index++) {
      assertThat(generator.generate()).hasSize(7).matches("[A-Za-z0-9]{7}");
    }
  }
}
