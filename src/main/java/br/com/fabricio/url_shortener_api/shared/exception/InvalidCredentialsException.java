package br.com.fabricio.url_shortener_api.shared.exception;

public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Credenciais inválidas ou token ausente.");
  }
}
