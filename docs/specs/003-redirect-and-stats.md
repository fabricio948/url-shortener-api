# Spec 003 — Redirecionamento e Estatísticas

> Esta spec define o redirecionamento público do link curto e a consulta de
> estatísticas pelo dono. Depende das Specs 001 (login JWT) e 002 (link curto).
>
> Fontes: `docs/01-stack-e-arquitetura.md`, `docs/02-api-e-erros.md`,
> `docs/03-database.md`, `docs/04-qualidade-e-infra.md`,
> `docs/specs/001-auth-and-users.md`, `docs/specs/002-url-shortening.md` e as
> decisões registradas no fim deste arquivo.

## 1. Objetivo

- Redirecionar qualquer visitante de `/r/{shortCode}` para a URL original.
- Contar cada acesso no `click_count` na mesma transação.
- Permitir que o dono consulte as estatísticas do próprio link.

## 2. Fora de escopo

- Criar, listar e excluir links (Spec 002).
- Estatísticas agregadas de todos os links ou por período.
- Filtros, gráficos ou histórico de cliques (só existe o contador total).
- Excluir links expirados automaticamente.

## 3. Pacotes e arquivos propostos

| Arquivo | Responsabilidade |
|---|---|
| `url/RedirectController.java` | Rota pública `GET /r/{shortCode}`; devolve o 302. |
| `url/ShortUrlService.java` | Novos métodos `redirect` e `getStats` (mesmo service da Spec 002). |
| `url/ShortUrlStatsResponse.java` | DTO `record` de saída das estatísticas. |
| `url/ShortUrlRepository.java` | Reutiliza `findByShortCode` da Spec 002. |
| `url/ShortUrlController.java` | Recebe `GET /api/v1/urls/{shortCode}/stats`. |

- Nenhum arquivo novo de entidade: a tabela `short_urls` já existe na Spec 002.
- A rota `/r/{shortCode}` já está liberada como pública na configuração de
  segurança da Spec 001.

## 4. Endpoints

### 4.1 `GET /r/{shortCode}` — público

- Não exige token.
- Sucesso: **302 Found** com header `Location` apontando para a URL original e
  sem corpo.
- Cada acesso bem-sucedido incrementa `click_count` em 1.

| Situação | Status | `detail` |
|---|---|---|
| Código não existe | 404 | "Link não encontrado." |
| Link expirado | 410 | "Link expirado." |

Exemplo de resposta de sucesso:

```
HTTP/1.1 302 Found
Location: https://www.exemplo.com/pagina-muito-longa
```

### 4.2 `GET /api/v1/urls/{shortCode}/stats` — protegido

Exige `Authorization: Bearer <token>`. Apenas o dono do link pode consultar.

Resposta **200 OK** com `ShortUrlStatsResponse`:

```json
{
  "shortCode": "meu-link",
  "originalUrl": "https://www.exemplo.com/pagina-muito-longa",
  "clickCount": 12,
  "createdAt": "2026-11-26T14:30:00",
  "expiresAt": "2026-11-26T14:35:00"
}
```

| Situação | Status |
|---|---|
| Sem token / token inválido | 401 |
| Link de outro usuário | 403 |
| Código não existe | 404 |

## 5. Regras de negócio

### 5.1 Redirecionar (`ShortUrlService.redirect`)

1. `findByShortCode(shortCode)`; se não existir → **404** "Link não encontrado.".
2. Se `expiresAt` for menor ou igual a agora (`LocalDateTime.now(clock)`) → **410**
   "Link expirado." **sem** incrementar o contador.
3. Incrementar `clickCount` em 1 e salvar **na mesma transação** (`@Transactional`).
4. Devolver a `originalUrl` para o Controller montar o 302.

- Usar **302 Found** (temporário), como definido em `docs/02`; não usar 301.
- Dois acessos simultâneos podem perder um incremento — limitação aceita em
  `docs/03`.
- O redirect não exige login e não verifica o dono.

### 5.2 Estatísticas (`ShortUrlService.getStats`)

1. Receber o `userId` autenticado e o `shortCode`.
2. `findByShortCode(shortCode)`; se não existir → **404** "Link não encontrado.".
3. Se o dono for outro usuário → **403** "Você não tem permissão para acessar
   este link.".
4. Devolver `ShortUrlStatsResponse` com os dados atuais do link.

- Link expirado **continua** com estatísticas acessíveis ao dono (só o redirect
  deixa de funcionar).
- `clickCount` é o total acumulado desde a criação.

## 6. Erros

Mesmo formato das specs anteriores: `ProblemDetail` + `application/problem+json`,
com o único `GlobalExceptionHandler` em `shared/exception/`.

| Exceção (reaproveitada) | Status | `detail` |
|---|---|---|
| `ResourceNotFoundException` | 404 | "Link não encontrado." |
| `ResourceAccessDeniedException` | 403 | "Você não tem permissão para acessar este link." |
| `LinkExpiredException` | 410 | "Link expirado." |

- `LinkExpiredException` é criada nesta spec.
- 401 continua sendo tratado pela segurança (Spec 001).

## 7. Persistência

- Sem colunas novas. Apenas atualiza `short_urls.click_count`.
- Incremento e `save` dentro do `@Transactional` do Service.
- Nenhum `nativeQuery`; usar o `save` do repositório.

## 8. Testes (`ShortUrlServiceTest`)

Unitários, JUnit 5 + Mockito, sem Spring/banco/Docker, `Clock.fixed` em UTC.

| Caso | Esperado |
|---|---|
| Redirecionar link válido | Devolve a `originalUrl` e incrementa `clickCount` em 1 |
| Redirecionar código inexistente | `ResourceNotFoundException` (404) |
| Redirecionar link expirado | `LinkExpiredException` (410) e **não** incrementa |
| Estatísticas do dono | Devolve `ShortUrlStatsResponse` com os valores do link |
| Estatísticas de link de outro usuário | `ResourceAccessDeniedException` (403) |
| Estatísticas de código inexistente | `ResourceNotFoundException` (404) |
| Estatísticas de link expirado | Continua devolvendo 200 (somente o dono) |

## 9. Critérios de aceite

- [ ] `GET /r/{shortCode}` redireciona com 302 e header `Location` correto.
- [ ] Cada acesso válido incrementa `click_count` em 1.
- [ ] Código inexistente devolve 404; expirado devolve 410 sem incrementar.
- [ ] `GET /api/v1/urls/{shortCode}/stats` devolve 200 para o dono.
- [ ] Link de outro usuário devolve 403; inexistente devolve 404.
- [ ] Estatísticas de link expirado continuam acessíveis ao dono.
- [ ] A rota de redirect funciona sem token; a de estatísticas exige token.
- [ ] Nenhum `@Entity` é devolvido pela API.

## 10. Decisões desta spec (revisar antes de implementar)

1. **302 Found** para o redirect, conforme `docs/02`.
2. **Expiração**: link é considerado expirado quando `expiresAt` é menor ou igual
   a agora (hora do `Clock`).
3. **Contador não incrementa** em acesso a link expirado.
4. **Rota de estatísticas**: `GET /api/v1/urls/{shortCode}/stats`, coerente com a
   exclusão por `shortCode` da Spec 002.
5. **Resposta das estatísticas**: `shortCode`, `originalUrl`, `clickCount`,
   `createdAt`, `expiresAt`.
6. **Link expirado mantém estatísticas** acessíveis ao dono.
7. **Sem histórico de cliques**: apenas o contador total, já definido no banco.
8. **Controller separado** (`RedirectController`) porque a rota pública fica fora
   de `/api/v1`.
