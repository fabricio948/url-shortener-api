package br.com.fabricio.url_shortener_api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException {
    writeProblem(
        request,
        response,
        HttpServletResponse.SC_UNAUTHORIZED,
        "Unauthorized",
        "Credenciais inválidas ou token ausente.");
  }

  private void writeProblem(
      HttpServletRequest request,
      HttpServletResponse response,
      int status,
      String title,
      String detail)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    String body =
        "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"%s\"}"
            .formatted(escape(title), status, escape(detail), escape(request.getRequestURI()));
    response.getWriter().write(body);
  }

  private String escape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
