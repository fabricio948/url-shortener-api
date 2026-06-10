package com.urlshortener.api.service;

import com.urlshortener.api.domain.UrlMapping;
import com.urlshortener.api.dto.ShortenUrlRequest;
import com.urlshortener.api.dto.ShortenUrlResponse;
import com.urlshortener.api.repository.UrlMappingRepository;
import com.urlshortener.api.service.impl.UrlShortenerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlShortenerServiceImpl service;

    private ShortenUrlRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ShortenUrlRequest("Fabricio", "fabricio@email.com", "https://google.com");
    }

    @Test
    @DisplayName("Should shorten URL successfully when user is under the link limit")
    void shortenUrlSuccess() {
        // Arrange (Configurando os dublês do Mockito)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("limit:fabricio@email.com")).thenReturn("0"); // Usuário tem 0 links ativos
        when(repository.save(any(UrlMapping.class))).thenReturn(new UrlMapping());

        // Act (Executando a ação real do serviço)
        ShortenUrlResponse response = service.shortenUrl(validRequest);

        // Assert (Validando as respostas e se os bancos fictícios foram chamados)
        assertNotNull(response);
        assertTrue(response.shortUrl().contains("http://localhost:8080/r/"));
        assertEquals("https://google.com", response.originalUrl());

        verify(repository, times(1)).save(any(UrlMapping.class));
        verify(valueOperations, times(1)).set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when user has reached the strict limit of 3 active links")
    void shortenUrlThrowsExceptionWhenLimitReached() {
        // Arrange (Configurando o Mockito para dizer que o usuário já tem 3 links ativos)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("limit:fabricio@email.com")).thenReturn("3");

        // Act & Assert (Executa a ação esperando uma falha controlada do tipo IllegalStateException)
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            service.shortenUrl(validRequest);
        });

        // Valida se a mensagem de erro customizada está correta
        assertEquals("User has reached the strict limit of 3 active links.", exception.getMessage());

        // Garante que o sistema travou ANTES de salvar qualquer dado no MongoDB ou Redis
        verify(repository, never()).save(any(UrlMapping.class));
        verify(valueOperations, never()).set(anyString(), anyString(), any());
    }
}
