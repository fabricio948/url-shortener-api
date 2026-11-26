package br.com.fabricio.url_shortener_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "não pode estar em branco")
        @Email(message = "deve ser um e-mail válido")
        String email,
    @NotBlank(message = "não pode estar em branco") String password) {
}
