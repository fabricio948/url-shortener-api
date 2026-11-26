package br.com.fabricio.url_shortener_api.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException() {
    super("Link não encontrado.");
  }
}
