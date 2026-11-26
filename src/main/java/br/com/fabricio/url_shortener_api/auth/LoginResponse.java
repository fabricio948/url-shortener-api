package br.com.fabricio.url_shortener_api.auth;

public record LoginResponse(String token, String tokenType, long expiresIn) {
}
