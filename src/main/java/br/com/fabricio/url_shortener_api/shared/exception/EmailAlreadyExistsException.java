package br.com.fabricio.url_shortener_api.shared.exception;

public class EmailAlreadyExistsException extends RuntimeException {

  public EmailAlreadyExistsException() {
    super("E-mail já cadastrado.");
  }
}
