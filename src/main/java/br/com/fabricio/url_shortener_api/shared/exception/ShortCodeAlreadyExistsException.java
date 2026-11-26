package br.com.fabricio.url_shortener_api.shared.exception;

public class ShortCodeAlreadyExistsException extends RuntimeException {

  public ShortCodeAlreadyExistsException() {
    super("Código curto já existe.");
  }
}
