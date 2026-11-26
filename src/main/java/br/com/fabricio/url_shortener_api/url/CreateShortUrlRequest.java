package br.com.fabricio.url_shortener_api.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
    @NotBlank(message = "não pode estar em branco")
        @Size(max = 2048, message = "deve ter no máximo 2048 caracteres")
        @Pattern(
            regexp = "^https?://.+$",
            message = "deve começar com http:// ou https://")
        String originalUrl,
    @Size(min = 3, max = 20, message = "deve ter entre 3 e 20 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9]+$", message = "deve conter apenas letras e números")
        String customAlias) {
}
