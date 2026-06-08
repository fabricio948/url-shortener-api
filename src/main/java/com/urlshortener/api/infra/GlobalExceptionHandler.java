package com.urlshortener.api.infra;


import com.urlshortener.api.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Captura quando o link expirou ou não existe (IllegalArgumentException lançado no Service)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundOrExpired(IllegalArgumentException ex) {
        log.warn("[EXCEPTION HANDLED] Resource not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 2. Captura quando o usuário atingiu o limite de 5 encurtamentos (IllegalStateException lançado no Service)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleLimitExceeded(IllegalStateException ex) {
        log.warn("[EXCEPTION HANDLED] Business rule violation: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    // 3. Captura erros de validação física dos DTOs (como e-mail sem @ ou final diferente de .com, campos nulos, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Usando a Stream API moderna do Java para coletar e concatenar todas as mensagens de erro em uma única String
        String errorMessage = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" | "));

        log.warn("[EXCEPTION HANDLED] Validation failure: {}", errorMessage);

        ErrorResponse error = new ErrorResponse(
                errorMessage,
                HttpStatus.BAD_REQUEST.value(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
