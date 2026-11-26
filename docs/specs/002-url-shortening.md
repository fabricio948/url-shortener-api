# Spec 002 — Encurtamento de URLs

> Esta spec define criar, listar e excluir links curtos. Depende da Spec 001
> (usuário autenticado via JWT).
>
> Fontes: `docs/01-stack-e-arquitetura.md`, `docs/02-api-e-erros.md`,
> `docs/03-database.md`, `docs/04-qualidade-e-infra.md`,
> `docs/specs/001-auth-and-users.md` e as decisões registradas no fim deste arquivo.

## 1. Objetivo

- Criar um link curto para o usuário autenticado.
- Aceitar um alias personalizado opcional; se não vier, gerar um código aleatório.
- Listar somente os links do usuário autenticado.
- Excluir um link do próprio usuário.
- Aplicar prazo de expiração (TTL) e limite de 5 links **ativos** por usuário.

## 2. Fora de escopo

- Redirecionar e contar cliques (Spec 003).
- Estatísticas (Spec 003).
- Editar um link depois de criado.
- Paginação da listagem.
- Recuperar/renovar link expirado.
- Deletar links expirados automaticamente.

## 3. Pacotes e arquivos propostos

Dentro de `br.com.fabricio.url_shortener_api`.

| Arquivo | Responsabilidade |
|---|---|
| `url/ShortUrl.java` | Entidade `@Entity` da tabela `short_urls` (sem regra de negócio). |
| `url/ShortUrlRepository.java` | Consultas derivadas (ver seção 7). |
| `url/ShortUrlController.java` | Recebe HTTP, valida com `@Valid`, chama o service. |
| `url/ShortUrlService.java` | Regras de criação, listagem e exclusão, com `@Transactional`. |
| `url/ShortCodeGenerator.java` | Interface com um único método: `String generate()`. |
| `url/RandomShortCodeGenerator.java` | Implementação de produção com `SecureRandom`. |
| `url/CreateShortUrlRequest.java` | DTO `record` de entrada. |
| `url/ShortUrlResponse.java` | DTO `record` de saída. |
| `shared/exception/` | Exceções de negócio desta spec (ver seção 6). |

- O id do usuário autenticado vem do `SecurityContext` (principal = id do usuário,
  definido na Spec 001).
- Injeção por construtor, campos `private final`, um único construtor.
- Controller não acessa Repository; `@Entity` não sai do Service.

## 4. Endpoints

Todos exigem `Authorization: Bearer <token>`.

### 4.1 `POST /api/v1/urls` — criar link

Request:

```json
{
  "originalUrl": "https://www.exemplo.com/pagina-muito-longa",
  "customAlias": "meu-link"
}
```

`customAlias` é opcional: omita o campo para o servidor gerar o código.

`CreateShortUrlRequest` (`record`):

| Campo | Validação |
|---|---|
| `originalUrl` | `@NotBlank`, `@Size(max = 2048)`, `@Pattern(regexp = "^https?://.+$")` |
| `customAlias` | Opcional. Se enviado: `@Size(min = 3, max = 20)` e `@Pattern(regexp = "^[A-Za-z0-9]+$")` |

- URL sem `http://` ou `https://` → **400**.
- Alias com caractere inválido ou tamanho fora de 3–20 → **400**.

Resposta **201 Created** com `ShortUrlResponse`:

```json
{
  "id": 10,
  "originalUrl": "https://www.exemplo.com/pagina-muito-longa",
  "shortCode": "meu-link",
  "clickCount": 0,
  "expiresAt": "2026-11-26T14:35:00",
  "createdAt": "2026-11-26T14:30:00"
}
```

Erros: 400 (validação), 401 (sem token), 409 (alias já existe), 422 (limite de
links ativos).

### 4.2 `GET /api/v1/urls` — listar links do usuário

Resposta **200 OK** com um array de `ShortUrlResponse`, ordenado por `createdAt`
decrescente (mais recentes primeiro):

```json
[
  {
    "id": 10,
    "originalUrl": "https://www.exemplo.com/pagina-muito-longa",
    "shortCode": "meu-link",
    "clickCount": 0,
    "expiresAt": "2026-11-26T14:35:00",
    "createdAt": "2026-11-26T14:30:00"
  }
]
```

- Devolve **todos** os links do usuário, inclusive os expirados (que continuam no
  banco até serem excluídos).
- Sem links: **200** com `[]`.

Erros: 401 (sem token).

### 4.3 `DELETE /api/v1/urls/{shortCode}` — excluir link

- Sucesso: **204 No Content**, sem corpo.
- Erros: 401 (sem token), 403 (link de outro usuário), 404 (código inexistente).

## 5. Regras de negócio (`ShortUrlService`)

### 5.1 Criar

1. Descobrir o `userId` autenticado.
2. Contar links **ativos** do usuário:
   `countByUserIdAndExpiresAtAfter(userId, now)`; se for **≥ 5** → **422** com
   `detail` "Limite de 5 links ativos atingido. Exclua um link ou aguarde um expirar.".
3. Definir o `shortCode`:
   - Se `customAlias` foi enviado: usar o valor; se `existsByShortCode` → **409**
     com `detail` "Código curto já existe.".
   - Se não foi enviado: usar `ShortCodeGenerator.generate()`; se houver colisão,
     gerar de novo (no máximo 5 tentativas) e, se ainda colidir, **409**.
4. `expiresAt` = `LocalDateTime.now(clock).plusSeconds(ttlSeconds)`.
5. `createdAt` = `LocalDateTime.now(clock)` (UTC).
6. `clickCount` = `0`.
7. Salvar e devolver `ShortUrlResponse`.

Se duas requisições concorrentes passarem pela contagem ao mesmo tempo, o limite
pode ser ultrapassado — limitação aceita em `docs/03`.

### 5.2 Listar

- Buscar por `userId` ordenando por `createdAt` decrescente.
- Converter cada `ShortUrl` em `ShortUrlResponse` dentro do service.

### 5.3 Excluir

1. `findByShortCode(shortCode)`; se não existir → **404** com `detail`
   "Link não encontrado.".
2. Se o dono do link for outro usuário → **403** com `detail`
   "Você não tem permissão para acessar este link.".
3. Excluir e devolver vazio (204).

### 5.4 Geração de código (`RandomShortCodeGenerator`)

- 1 método: `String generate()`.
- 7 caracteres, alfabeto Base62 (`A–Z`, `a–z`, `0–9`), usando `SecureRandom`.
- Cabe com folga no `VARCHAR(20)` definido em `docs/03`.

## 6. Erros

Mesmo formato da Spec 001: `ProblemDetail` + `application/problem+json`, tratado
pelo único `GlobalExceptionHandler`.

| Exceção de negócio (proposta) | Status | `detail` |
|---|---|---|
| `ShortCodeAlreadyExistsException` | 409 | "Código curto já existe." |
| `ActiveLinkLimitReachedException` | 422 | "Limite de 5 links ativos atingido. Exclua um link ou aguarde um expirar." |
| `ResourceNotFoundException` | 404 | "Link não encontrado." |
| `ResourceAccessDeniedException` | 403 | "Você não tem permissão para acessar este link." |

`ResourceNotFoundException` e `ResourceAccessDeniedException` são criadas nesta
spec e depois reutilizadas pela Spec 003.

## 7. Persistência

Tabela `short_urls`, conforme `docs/03-database.md`:

| Coluna | Java | Regra |
|---|---|---|
| `id` | `Long` | PK, `GenerationType.IDENTITY` |
| `original_url` | `String` | `nullable = false`, `length = 2048` |
| `short_code` | `String` | `nullable = false`, `length = 20`, único (`idx_short_code`) |
| `click_count` | `long` | `nullable = false`, inicia em 0 |
| `expires_at` | `LocalDateTime` | `nullable = false` |
| `created_at` | `LocalDateTime` | `nullable = false` |
| `user_id` | `User` | `@ManyToOne(fetch = FetchType.LAZY)`, `nullable = false` |

- `@Table(name = "short_urls", indexes = @Index(name = "idx_short_code", columnList = "short_code", unique = true))`.
- Consultas derivadas, sem JPQL e sem SQL nativo:
  - `Optional<ShortUrl> findByShortCode(String shortCode)`
  - `boolean existsByShortCode(String shortCode)`
  - `long countByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now)`
  - `List<ShortUrl> findByUserIdOrderByCreatedAtDesc(Long userId)`

## 8. Configuração

Em `src/main/resources/application.properties` (valores definidos em `docs/03`):

```properties
app.links.ttl-seconds=300
app.links.max-per-user=5
```

Lidos no Service via `@Value` ou `@ConfigurationProperties`; nunca fixos no código.

## 9. Testes (`ShortUrlServiceTest`)

Unitários, JUnit 5 + Mockito, sem Spring/banco/Docker, `Clock.fixed` em UTC.

| Caso | Esperado |
|---|---|
| Criar com URL válida e sem alias | Gera código, `clickCount = 0`, `expiresAt = agora + 300 s` |
| Criar com `customAlias` livre | Usa o alias enviado |
| `customAlias` já existente | `ShortCodeAlreadyExistsException` (409) |
| 5 links ativos | `ActiveLinkLimitReachedException` (422) |
| 5 links, mas alguns expirados | Cria normalmente (expirados não contam) |
| Listar | Devolve só os links do usuário, mais recentes primeiro |
| Excluir link próprio | Exclui, sem erro |
| Excluir link inexistente | `ResourceNotFoundException` (404) |
| Excluir link de outro usuário | `ResourceAccessDeniedException` (403) |

Também testar o `RandomShortCodeGenerator`: tamanho 7 e apenas caracteres Base62.

## 10. Critérios de aceite

- [ ] `POST /api/v1/urls` cria o link e devolve 201 com o `ShortUrlResponse`.
- [ ] Sem `customAlias`, o código é gerado com 7 caracteres Base62.
- [ ] Alias duplicado devolve 409.
- [ ] 5 links ativos bloquearam a criação com 422; expirados não contam.
- [ ] `expiresAt` = hora atual (do `Clock`) + TTL configurado.
- [ ] `GET /api/v1/urls` devolve só os links do usuário autenticado.
- [ ] `DELETE /api/v1/urls/{shortCode}` devolve 204, 403 ou 404 conforme o caso.
- [ ] URL sem `http/https` devolve 400.
- [ ] Nenhum `@Entity` é devolvido pela API.

## 11. Decisões desta spec (revisar antes de implementar)

1. **Alias personalizado:** opcional, campo `customAlias`, 3–20 caracteres,
   apenas letras e números (`[A-Za-z0-9]`). Sem hífen para manter simples.
2. **Exclusão por `{shortCode}`**, alinhada aos exemplos do `docs/02`
   (`/api/v1/urls/aB9x2kL`).
3. **Código gerado:** 7 caracteres, Base62 (A–Z, a–z, 0–9).
4. **Listagem:** array simples, sem paginação, ordenado do mais recente para o
   mais antigo; inclui links expirados.
5. **Resposta `ShortUrlResponse`:** `id`, `originalUrl`, `shortCode`, `clickCount`,
   `expiresAt`, `createdAt`.
6. **Sem link completo na resposta:** devolvemos só `shortCode`; montar
   `https://.../r/{shortCode}` exigiria uma URL base configurável, que os docs
   não definem.
7. **Mensagem de 409 para alias:** "Código curto já existe."
8. **Tentativas de geração aleatória:** até 5 antes de desistir com 409.
