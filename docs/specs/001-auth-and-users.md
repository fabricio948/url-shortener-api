# Spec 001 — Autenticação e Usuários

> Esta spec define cadastro e login com JWT. É a base para todas as rotas
> protegidas das specs 002 e 003.
>
> Fontes: `docs/01-stack-e-arquitetura.md`, `docs/02-api-e-erros.md`,
> `docs/03-database.md`, `docs/04-qualidade-e-infra.md` e as decisões registradas
> no fim deste arquivo.

## 1. Objetivo

- Cadastrar usuário com senha armazenada em hash BCrypt.
- Autenticar usuário e devolver um token JWT.
- Proteger as rotas de `/api/v1/**` (exceto as públicas) com o token.
- Tratar 401/403 dentro do Spring Security no mesmo formato de erro da API
  (`ProblemDetail`), porque essas falhas acontecem antes do Controller.

## 2. Fora de escopo

- Criar, listar, excluir ou redirecionar links (specs 002 e 003).
- Estatísticas de cliques (spec 003).
- Refresh token, logout e recuperação de senha.
- Confirmação de e-mail.
- Papéis/perfis de usuário (roles). Todos os usuários autenticados têm o mesmo nível.

## 3. Pacotes e arquivos propostos

Dentro de `br.com.fabricio.url_shortener_api` (package base existente, não criar outro).

| Arquivo | Responsabilidade |
|---|---|
| `user/User.java` | Entidade `@Entity` da tabela `users` (sem regra de negócio). |
| `user/UserRepository.java` | `existsByEmail(String)`, `findByEmail(String)`. |
| `auth/AuthController.java` | Recebe HTTP, valida com `@Valid`, chama o service. |
| `auth/AuthService.java` | Regras de cadastro e login com `@Transactional`. |
| `auth/JwtService.java` | Gera e valida o token; lê `JWT_SECRET` e a expiração. |
| `auth/JwtAuthenticationFilter.java` | Lê `Authorization: Bearer`, valida e autentica. |
| `auth/RestAuthenticationEntryPoint.java` | 401 no formato `ProblemDetail`. |
| `auth/RestAccessDeniedHandler.java` | 403 no formato `ProblemDetail`. |
| `auth/RegisterRequest.java` | DTO `record` de entrada do cadastro. |
| `auth/LoginRequest.java` | DTO `record` de entrada do login. |
| `auth/LoginResponse.java` | DTO `record` de saída do login. |
| `auth/UserResponse.java` | DTO `record` de saída do cadastro. |
| `shared/config/SecurityConfig.java` | `SecurityFilterChain` e bean de `PasswordEncoder`. |
| `shared/config/ClockConfig.java` | Bean `Clock` em UTC. |
| `shared/exception/GlobalExceptionHandler.java` | Único `@RestControllerAdvice`. |
| `shared/exception/` | Exceções de negócio (ver seção 7). |

Injeção por construtor, campos `private final`, um único construtor. Nunca
`@Autowired` em campo. Controller não acessa Repository.

## 4. Endpoints

### 4.1 `POST /api/v1/auth/register` — público

Request (`Content-Type: application/json`):

```json
{
  "name": "Fabricio",
  "email": "fabricio@example.com",
  "password": "senha1234"
}
```

`RegisterRequest` (`record`) com validação Jakarta:

| Campo | Validação |
|---|---|
| `name` | `@NotBlank` |
| `email` | `@NotBlank` `@Email` |
| `password` | `@NotBlank` `@Size(min = 8)` |

Resposta **201 Created** com `UserResponse`:

```json
{
  "id": 1,
  "name": "Fabricio",
  "email": "fabricio@example.com"
}
```

A senha **nunca** aparece em nenhuma resposta.

Erros:

| Situação | Status |
|---|---|
| Campo ausente/ inválido (ex.: senha com menos de 8) | 400 |
| E-mail já cadastrado | 409 |

### 4.2 `POST /api/v1/auth/login` — público

Request:

```json
{
  "email": "fabricio@example.com",
  "password": "senha1234"
}
```

`LoginRequest` (`record`): `email` `@NotBlank` `@Email`; `password` `@NotBlank`.

Resposta **200 OK** com `LoginResponse`:

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 300
}
```

- `expiresIn` é o tempo de vida do token **em segundos**.
- Erros: 400 para campo inválido; 401 para credencial errada (mensagem genérica).

## 5. Regras de negócio

### 5.1 Cadastro (`AuthService.register`)

1. Se `userRepository.existsByEmail(email)` → lançar exceção de conflito → **409**
   com `detail` "E-mail já cadastrado.".
2. Normalizar o e-mail com `trim()` e `toLowerCase()` antes de checar e salvar.
3. `password` é gravado **somente** como hash BCrypt.
4. `createdAt` recebe `LocalDateTime.now(clock)` (UTC); o banco não preenche datas.
5. Salvar e devolver `UserResponse` (sem senha).

Nota: se duas requisições concorrentes passarem pela checagem, a restrição
`unique` do banco ainda garante o 409 — traduzir a violação de integridade para a
mesma exceção de conflito.

### 5.2 Login (`AuthService.login`)

1. `userRepository.findByEmail(email)`.
2. Se o usuário não existir **ou** a senha não conferir com o hash, lançar a
   **mesma** exceção → **401** com `detail` "Credenciais inválidas ou token
   ausente." (não revelar se o e-mail existe).
3. Gerar o token via `JwtService` e devolver `LoginResponse`.

### 5.3 Token JWT (`JwtService`)

- Algoritmo **HS256** com `JWT_SECRET` (HS256 exige no mínimo 256 bits).
- `JWT_SECRET` é obrigatório e vem de variável de ambiente; **sem valor padrão**.
  Se faltar ou tiver menos de 32 caracteres, a aplicação **falha ao iniciar**.
- `subject` = `users.id` convertido para texto.
- Expiração = agora (do `Clock`) + `app.jwt.expiration-seconds`.
- `tokenType` retornado é a string `"Bearer"`.
- Validação: assinatura correta e `exp` no futuro. Token inválido, expirado ou
  ausente → **401** no formato `ProblemDetail`.

## 6. Segurança

`SecurityConfig`:

- Sessão **stateless** (`SessionCreationPolicy.STATELESS`); sem formulário de login;
  CSRF desabilitado (API sem cookies de sessão).
- Rotas públicas: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`,
  `GET /r/{shortCode}`.
- Qualquer outra rota exige autenticação (`Authorization: Bearer <token>`).
  Rotas novas nascem protegidas, conforme `docs/02`.
- `JwtAuthenticationFilter` roda antes do `UsernamePasswordAuthenticationFilter`:
  lê o header, valida o token e popula o `SecurityContext` com o id do usuário
  como principal (sem consultar o banco a cada requisição).
- `RestAuthenticationEntryPoint` devolve **401** e `RestAccessDeniedHandler`
  devolve **403**, ambos em `ProblemDetail` (`application/problem+json`), porque
  o `@RestControllerAdvice` não intercepta essas falhas.
- Bean `PasswordEncoder` = `BCryptPasswordEncoder`.

## 7. Erros

Formato único `ProblemDetail` (RFC 9457), `Content-Type: application/problem+json`,
campos `type`, `title`, `status`, `detail`, `instance`. `detail` em PT-BR e sem
detalhe interno (sem stack trace, classe, SQL).

| Exceção de negócio (proposta) | Status | `detail` |
|---|---|---|
| `EmailAlreadyExistsException` | 409 | "E-mail já cadastrado." |
| `InvalidCredentialsException` | 401 | "Credenciais inválidas ou token ausente." |
| `MethodArgumentNotValidException` | 400 | genérico + `errors[]` |
| Qualquer exceção não tratada | 500 | "Erro interno no servidor." |

Para 400, o `ProblemDetail` inclui o campo de extensão `errors`:

```json
{
  "type": "about:blank",
  "title": "Requisição inválida",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/v1/auth/register",
  "errors": [
    { "field": "password", "message": "deve ter no mínimo 8 caracteres" }
  ]
}
```

`GlobalExceptionHandler` fica em `shared/exception/` e é o **único**
`@RestControllerAdvice` do projeto. Os handlers 401/403 da segurança ficam em
`auth/` e produzem o mesmo formato.

## 8. Persistência

Tabela `users`, conforme `docs/03-database.md`:

| Coluna | Java | Regra |
|---|---|---|
| `id` | `Long` | PK, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `name` | `String` | `nullable = false` |
| `email` | `String` | `nullable = false`, `unique = true` |
| `password` | `String` | `nullable = false`, guarda só o hash BCrypt |
| `created_at` | `LocalDateTime` | Preenchido pelo Service com `Clock` (UTC) |

- `@Table(name = "users")`.
- Entidade **sem** regra de negócio e sem DTO exposto.
- `ddl-auto=update`; **não** criar Flyway nesta fase.

## 9. Configuração

Em `src/main/resources/application.properties`:

```properties
# TEMPORÁRIO: 5 minutos (300 s), só para testes. Troque aqui para o prazo definitivo.
app.jwt.expiration-seconds=300
```

- `JWT_SECRET` (secreto) entra em `.env`, `.env.example` e `docker-compose.yml`
  **durante a implementação**, com o diff mostrado e aprovado antes:
  - `.env`: valor real gerado com `openssl rand -base64 48`.
  - `.env.example`: valor falso de exemplo.
  - compose (`api`): `JWT_SECRET: ${JWT_SECRET:?defina JWT_SECRET no .env}`.
- Nenhum valor secreto no código, no `application.properties` ou no compose.

## 10. Dependências

Adicionar ao `pom.xml` na implementação (após conferir a versão):

| Dependência | Por quê |
|---|---|
| `org.springframework.boot:spring-boot-starter-security` | Filtro, `PasswordEncoder`, configuração de rotas, entry point/handler. |
| `com.auth0:java-jwt` | Geração e validação do token JWT (escolha do usuário). |

Antes de editar o `pom.xml`, conferir os nomes exatos para o Spring Boot 4.2 e
mostrar o diff para aprovação.

## 11. Testes (`AuthServiceTest`)

Unitários do Service, JUnit 5 + Mockito, **sem** Spring, banco ou Docker.
Given/When/Then com comentário curto em PT-BR; nomes de teste em inglês.

| Caso | Esperado |
|---|---|
| Cadastro válido | Salva usuário e devolve `UserResponse` |
| E-mail duplicado | Lança `EmailAlreadyExistsException` (409) |
| Senha persistida | O valor salvo não é a senha em texto puro e confere com o BCrypt |
| Login com senha errada | Lança `InvalidCredentialsException` (401) |
| Login com e-mail inexistente | Mesma exceção e mensagem do caso anterior |
| Expiração do token | Com `Clock.fixed`, `expiresIn` = 300 e `exp` = agora + 300 s |

- `Clock.fixed` sempre em UTC; nunca `Thread.sleep`.
- Testar também `JwtService` para token válido, token expirado e assinatura inválida.

## 12. Critérios de aceite

- [ ] `POST /api/v1/auth/register` cria usuário e devolve 201 sem senha.
- [ ] E-mail duplicado devolve 409 com `detail` "E-mail já cadastrado.".
- [ ] Senha gravada só como hash BCrypt.
- [ ] `POST /api/v1/auth/login` devolve 200 com `token`, `tokenType` e `expiresIn`.
- [ ] Credencial errada devolve 401 com mensagem genérica (não revela o e-mail).
- [ ] Rotas de `/api/v1/**` que não sejam as públicas exigem Bearer válido.
- [ ] Token ausente/inválido/expirado devolve 401 em `application/problem+json`.
- [ ] 403 da segurança também usa `ProblemDetail`.
- [ ] `JWT_SECRET` ausente ou curto impede o start da aplicação.
- [ ] Todos os erros usam um único `@RestControllerAdvice` + handlers da segurança.
- [ ] Testes do `AuthService` e do `JwtService` passando via Docker.

## 13. Decisões desta spec (revisar antes de implementar)

Itens que as docs não fixavam e que esta spec resolveu:

1. **Expiração do token:** 300 s (5 min), via `app.jwt.expiration-seconds`, com
   comentário no `application.properties` indicando onde trocar (decisão do usuário).
2. **Biblioteca JWT:** `com.auth0:java-jwt` (decisão do usuário).
3. **Resposta do login:** `token` + `tokenType: "Bearer"` + `expiresIn` em segundos
   (decisão do usuário).
4. **Senha:** mínimo de 8 caracteres (decisão do usuário).
5. **Resposta do cadastro:** 201 com `UserResponse` (`id`, `name`, `email`).
6. **`subject` do token:** id do usuário; o filtro não consulta o banco por requisição.
7. **Local do `SecurityConfig`:** `shared/config/` (bean de configuração).
8. **Nomes das classes e exceções:** os listados nas seções 3 e 7.
9. **CSRF desabilitado e sessão stateless** por ser uma API sem cookies de sessão.
10. **E-mail normalizado** com `trim()` + `toLowerCase()` no cadastro.
