package br.com.fabricio.url_shortener_api.shared.exception;

public class ActiveLinkLimitReachedException extends RuntimeException {

  public ActiveLinkLimitReachedException() {
    super("Limite de 5 links ativos atingido. Exclua um link ou aguarde um expirar.");
  }
}
