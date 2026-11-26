package br.com.fabricio.url_shortener_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "não pode estar em branco") String name,
    @NotBlank(message = "não pode estar em branco")
        @Email(message = "deve ser um e-mail válido")
        String email,
    @NotBlank(message = "não pode estar em branco")
        @Size(min = 8, message = "deve ter no mínimo 8 caracteres")
        String password) {
}
