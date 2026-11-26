# URL Shortener API

API REST para encurtar links, com cadastro e login via JWT. Projeto de estudo de
SDD (Spec-Driven Development).

- Cada link expira em 5 minutos (configurável em `app.links.ttl-seconds`).
- Cada usuário pode manter até 5 links ativos (configurável em `app.links.max-per-user`).
- O redirecionamento é público e cada acesso é contabilizado.
- O dono do link pode consultar as estatísticas de cliques.

## Stack

- Java 25 e Spring Boot 4.2
- Spring Web MVC, Spring Security (JWT com `java-jwt`) e Spring Data JPA
- MySQL 8.4
- Docker e Docker Compose
- Testes com JUnit 5 e Mockito

## Pré-requisitos

- Docker e Docker Compose.
- Java **não** precisa estar instalado na máquina: compilação e testes rodam em
  containers.

## Configuração do `.env`

Crie o arquivo a partir do exemplo:

```bash
cp .env.example .env
```

| Variável | Descrição |
|---|---|
| `DB_HOST` | Host do MySQL (use `localhost` fora do Docker) |
| `DB_PORT` | Porta do MySQL (padrão `3306`) |
| `DB_NAME` | Nome do banco |
| `DB_USER` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco — **obrigatória** |
| `SERVER_PORT` | Porta da API (padrão `8080`) |
| `JWT_SECRET` | Segredo do JWT — **obrigatório**, mínimo de 32 caracteres |

`DB_PASSWORD` e `JWT_SECRET` não têm valor padrão: sem eles a aplicação não
inicia. Gere o segredo com:

```bash
openssl rand -base64 48
```

O `.env` está no `.gitignore` e não deve ser commitado. O `.env.example` contém
apenas valores falsos. O Docker Compose lê o `.env` automaticamente; sem ele, o
`up` falha com a mensagem `defina DB_PASSWORD no .env`.

## Subir com Docker Compose

Com o `.env` criado, o comando que **roda o projeto** é o `up`. Ele compila a
imagem da API e sobe o MySQL e a API:

```bash
docker compose up -d --build
```

`-d` executa em segundo plano e `--build` recompila a imagem.

Confira se os containers estão de pé:

```bash
docker compose ps
```

O `mysql_dev` deve aparecer como `healthy` e o `spring_java25_api` como `Up`. A
API só inicia depois que o MySQL fica saudável (`healthcheck`).

Para ver as últimas 100 linhas do log da API (**não sobe nada**, apenas exibe):

```bash
docker compose logs --tail=100 api
```

Para acompanhar os logs ao vivo:

```bash
docker compose logs -f api
```

Se alterar apenas código Java, recompile e reinicie somente a API:

```bash
docker compose up -d --build api
```

- API: `http://localhost:8080` (ou `SERVER_PORT`)
- MySQL: `localhost:3306` (ou `DB_PORT`)

Para parar mantendo os dados:

```bash
docker compose down
```

**Atenção:** `docker compose down -v` apaga o volume do MySQL. Não use sem
querer perder os dados.

## Endpoints

Rotas protegidas exigem `Authorization: Bearer <token>`, obtido no login. Todas
as rotas da API ficam em `/api/v1/`; o redirecionamento é a exceção (`/r/...`).

| Método | Rota | Acesso | Sucesso |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Público | 201 |
| `POST` | `/api/v1/auth/login` | Público | 200 |
| `POST` | `/api/v1/urls` | Protegido | 201 |
| `GET` | `/api/v1/urls` | Protegido | 200 |
| `GET` | `/api/v1/urls/{shortCode}/stats` | Protegido | 200 |
| `DELETE` | `/api/v1/urls/{shortCode}` | Protegido | 204 |
| `GET` | `/r/{shortCode}` | Público | 302 |

### Exemplos com `curl`

**Cadastrar usuário**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Fabricio","email":"fabricio@example.com","password":"senha1234"}'
```

**Login** (guarde o `token` da resposta em `TOKEN`)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"fabricio@example.com","password":"senha1234"}'

TOKEN="<token>"
```

**Criar link curto** (o `customAlias` é opcional; sem ele o código é gerado)

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://www.exemplo.com/pagina-muito-longa","customAlias":"meu-link"}'
```

**Listar meus links**

```bash
curl http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $TOKEN"
```

**Consultar estatísticas de um link**

```bash
curl http://localhost:8080/api/v1/urls/meu-link/stats \
  -H "Authorization: Bearer $TOKEN"
```

**Excluir um link**

```bash
curl -X DELETE http://localhost:8080/api/v1/urls/meu-link \
  -H "Authorization: Bearer $TOKEN"
```

**Redirecionar** (público, sem token; o `-i` mostra o `302` e o header `Location`)

```bash
curl -i http://localhost:8080/r/meu-link
```

## Testes

Java só existe dentro do Docker. Rode a suíte com:

```bash
docker run --rm -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -w /app eclipse-temurin:25-jdk ./mvnw test
```

Os testes são unitários dos Services (JUnit 5 + Mockito), sem Spring, banco ou
Docker. O teste `UrlShortenerApiApplicationTests` gerado pelo Spring Initializr
foi removido por usar `@SpringBootTest`, que sobe o contexto do Spring; o motivo
está registrado em `docs/05-divergencias-e-decisoes.md`.
