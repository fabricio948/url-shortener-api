package br.com.fabricio.url_shortener_api.shared.exception;

public class LinkExpiredException extends RuntimeException {

  public LinkExpiredException() {
    super("Link expirado.");
  }
}
