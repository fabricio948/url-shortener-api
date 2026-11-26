package br.com.fabricio.url_shortener_api.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<Map<String, String>> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    Map.of(
                        "field",
                        error.getField(),
                        "message",
                        error.getDefaultMessage() == null
                            ? "valor inválido"
                            : error.getDefaultMessage()))
            .toList();

    ProblemDetail problem =
        createProblem(
            HttpStatus.BAD_REQUEST,
            "Requisição inválida",
            "Um ou mais campos estão inválidos.",
            request);
    problem.setProperty("errors", errors);
    return toResponse(HttpStatus.BAD_REQUEST, problem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return toResponse(
        HttpStatus.BAD_REQUEST,
        createProblem(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            "Dados inválidos na requisição.",
            request));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoResourceFound(
      NoResourceFoundException exception, HttpServletRequest request) {
    return toResponse(
        HttpStatus.NOT_FOUND,
        createProblem(
            HttpStatus.NOT_FOUND, "Not Found", "Recurso não encontrado.", request));
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ProblemDetail> handleEmailAlreadyExists(
      EmailAlreadyExistsException exception, HttpServletRequest request) {
    return toResponse(
        HttpStatus.CONFLICT,
        createProblem(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ProblemDetail> handleInvalidCredentials(
      InvalidCredentialsException exception, HttpServletRequest request) {
    return toResponse(
        HttpStatus.UNAUTHORIZED,
        createProblem(
            HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage(), request));
  }

  @ExceptionHandler(ShortCodeAlreadyExistsException.class)
  public ResponseEntity<ProblemDetail> handleShortCodeAlreadyExists(
      ShortCodeAlreadyExistsException exception, HttpServletRequest request) {
    return toResponse(
        HttpStatus.CONFLICT,
        createProblem(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request));
  }

  @ExceptionHandler(ActiveLinkLimitReachedException.class)
  public ResponseEntity<ProblemDetail> handleActiveLinkLimitReached(
      ActiveLinkLimitReachedException exception, HttpServletRequest request) {
    return toResponse(
        HttpStatus.UNPROCESSABLE_ENTITY,
        createProblem(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Unprocessable Entity",
            exception.getMessage(),
            request));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleResourceNotFound(
      ResourceNotFoundException exception, HttpServletRequest request) {
    return toResponse(
        HttpStatus.NOT_FOUND,
        createProblem(HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), request));
  }

  @ExceptionHandler(ResourceAccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleResourceAccessDenied(
      ResourceAccessDeniedException exception, HttpServletRequest request) {
    return toResponse(
        HttpStatus.FORBIDDEN,
        createProblem(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage(), request));
  }

  @ExceptionHandler(LinkExpiredException.class)
  public ResponseEntity<ProblemDetail> handleLinkExpired(
      LinkExpiredException exception, HttpServletRequest request) {
    return toResponse(
        HttpStatus.GONE,
        createProblem(HttpStatus.GONE, "Gone", exception.getMessage(), request));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    LOGGER.error("Erro inesperado ao processar a requisição", exception);
    return toResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        createProblem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "Erro interno no servidor.",
            request));
  }

  private ResponseEntity<ProblemDetail> toResponse(HttpStatus status, ProblemDetail problem) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private ProblemDetail createProblem(
      HttpStatus status, String title, String detail, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }
}
