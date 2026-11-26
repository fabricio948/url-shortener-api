package br.com.fabricio.url_shortener_api.shared.exception;

public class ResourceAccessDeniedException extends RuntimeException {

  public ResourceAccessDeniedException() {
    super("Você não tem permissão para acessar este link.");
  }
}
