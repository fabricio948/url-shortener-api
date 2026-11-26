# 05 — Divergências, correções e decisões

> **Documento auxiliar.** Não é uma spec e não altera `docs/01`–`04` nem
> `docs/specs/`. Registra o que divergiu entre a documentação (SDD) e o projeto
> real, o que foi corrigido, o que ficou pendente e o porquê. Serve de referência
> para projetos futuros.
>
> Última atualização: 2026-11-26.

## 1. Conclusão geral

Não foi encontrado conflito **entre** `docs/01`–`04` e as specs. As divergências
estavam entre a **documentação** e o **projeto inicial** (configuração gerada
antes das specs). As correções abaixo aproximaram o projeto do que os docs
determinam.

## 2. Resumo das divergências

| # | Divergência | Onde | Gravidade | Status |
|---|---|---|---|---|
| 1 | URL JDBC quebrada em duas linhas; parâmetros ignorados | `application.properties` | Alta | Corrigido |
| 2 | Fuso horário misturado (MySQL em UTC-3, JDBC local, JVM UTC) | `docker-compose.yml` + `application.properties` | Alta | Corrigido |
| 3 | Senha com valor padrão no compose e no `application.properties` | compose + properties | Alta | Corrigido |
| 4 | Healthcheck do MySQL com a senha na linha de comando | `docker-compose.yml` | Média | Corrigido |
| 5 | `DB_USER: root` fixo no compose, ignorando o `.env` | `docker-compose.yml` | Média | Corrigido |
| 6 | `spring.application.name` definido duas vezes | `application.properties` | Baixa | Corrigido |
| 7 | Teste gerado com `@SpringBootTest` contraria `docs/04` | `src/test/...` | Média | Removido |
| 8 | `.gitignore` não bloqueava `.env.*` | `.gitignore` | Média | Corrigido |
| 9 | `spring-boot-docker-compose` não previsto em nenhum doc | `pom.xml` | Baixa | Removido |
| 10 | Faltavam `spring-boot-starter-security` e `com.auth0:java-jwt` | `pom.xml` | Alta | Adicionado |
| 11 | `spring-boot-starter-test` não declarado, mas transitivo | `pom.xml` | Baixa | Sem ação |
| 12 | `JWT_SECRET` ausente no `.env`, `.env.example` e compose | — | Alta | Adiado p/ Spec 001 |
| 13 | `show-sql=true` sem perfil e `open-in-view` habilitado | `application.properties` | Baixa | Não alterado |
| 14 | Variáveis `DB_HOST`, `DB_PORT`, `SERVER_PORT` não listadas em `docs/04` | `.env` / compose | Baixa | Mantidas |
| 15 | `docs/01` dizia que o pom tinha Web/Validation/MySQL, mas o JPA já estava | `docs/01` | Baixa | Sem ação (doc desatualizada) |

## 3. Correções aplicadas

### 3.1 URL JDBC em uma única linha

**Problema:** em arquivo `.properties` não existe continuação de linha. A URL
terminava em `?` e os parâmetros caíam numa chave separada, sem valor.

Antes:

```properties
spring.datasource.url=jdbc:mysql://.../${DB_NAME:appdb}?
useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8
```

O log do Hibernate provava o descarte:

```
Database JDBC URL [jdbc:mysql://db:3306/appdb?]
```

Depois (uma linha só, fuso UTC):

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&characterEncoding=UTF-8
```

Log após a correção:

```
Database JDBC URL [jdbc:mysql://db:3306/appdb?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&characterEncoding=UTF-8]
```

`connectionTimeZone=UTC` é o nome atual do parâmetro no MySQL Connector/J 8;
`serverTimezone` era o antigo e estava sendo citado em `docs/03` apenas como
exemplo do que **não** fazer.

### 3.2 Fuso horário único em UTC

**Problema:** MySQL com `TZ: America/Sao_Paulo` (UTC-3), JVM da API em UTC e um
`serverTimezone` local na URL (que nem era aplicado). Com `LocalDateTime`, isso
mistura hora local e UTC e desloca a expiração dos links.

**Correção:**

- removido `TZ: America/Sao_Paulo` do serviço `db` (o container volta ao UTC padrão);
- API sem `TZ` (JVM já roda em UTC);
- `connectionTimeZone=UTC` na URL JDBC.

**Verificação:** `SELECT NOW(), UTC_TIMESTAMP()` passaram a devolver o mesmo
valor (ambos em UTC). `docs/03` recomenda exatamente esse arranjo.

### 3.3 Segredos obrigatórios, sem valor padrão

| Antes | Depois |
|---|---|
| `${DB_PASSWORD:-******}` no compose | `${DB_PASSWORD:?defina DB_PASSWORD no .env}` |
| `${DB_NAME:-appdb}` no compose | `${DB_NAME:?defina DB_NAME no .env}` |
| `DB_USER: root` fixo | `${DB_USER:?defina DB_USER no .env}` |
| `spring.datasource.password=${DB_PASSWORD:******}` | `spring.datasource.password=${DB_PASSWORD}` |
| `spring.datasource.username=${DB_USER:root}` | `spring.datasource.username=${DB_USER}` |
| `spring.datasource.url=...${DB_NAME:appdb}...` | `...${DB_NAME}...` |

**Atenção para projetos futuros:** a sintaxe `:?mensagem` é **do Docker Compose**.
No Spring, `${VAR:?mensagem}` seria lida como "use a mensagem como valor padrão".
Em `application.properties`, para tornar a variável obrigatória, use `${VAR}` sem
nenhum `:` — o Spring falha ao iniciar se ela não existir.

### 3.4 Healthcheck do MySQL sem senha no compose

Antes:

```yaml
test: [ "CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_PASSWORD:-******}" ]
```

Depois:

```yaml
test: [ "CMD-SHELL", "mysqladmin ping -h localhost -u root -p\"$$MYSQL_ROOT_PASSWORD\"" ]
```

`$$` é o escape do Compose para o `$` chegar ao shell do container. A senha não
aparece mais na configuração nem no `docker inspect`. Isso segue `docs/04`.

### 3.5 Nome da aplicação

`spring.application.name` estava definido duas vezes (`url-shortener-api` e
`api-java25`); a última vencia. Mantido apenas `url-shortener-api`.

### 3.6 Teste gerado removido

`UrlShortenerApiApplicationTests` usava `@SpringBootTest`, subindo o contexto do
Spring. `docs/04` determina testes **unitários de Service**, sem Spring, banco ou
Docker, e diz que testes de integração/Controller só existem se uma spec pedir.
Nenhuma pede. O arquivo foi removido e o motivo ficou registrado no `README.md`.

### 3.7 `.gitignore`

Antes só bloqueava `.env`. Agora:

```gitignore
.env
.env.*
!.env.example
```

A ordem importa: `.env.*` também casaria com `.env.example`, por isso o `!`
precisa vir depois.

### 3.8 `pom.xml`

**Adicionado** (conforme `docs/specs/001-auth-and-users.md` §10):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.6.2</version>
</dependency>
```

- `spring-boot-starter-security` não leva `<version>` (gerenciada pelo parent).
- `com.auth0:java-jwt` **não** é gerenciada pelo BOM do Boot 4.2.0, por isso a
  versão explícita. A 4.6.2 era a última estável em 2026-11-26.
- A java-jwt traz Jackson 2 em escopo runtime (2.22.3); o BOM do Boot gerencia
  Jackson 2 em 2.21.8, que é a versão efetivamente usada. Compatível.

**Removido:** `spring-boot-docker-compose` (runtime, optional). Não é citado por
nenhum doc/spec e não interferia no funcionamento dentro dos containers.

**Sem ação:** `spring-boot-starter-test`. Ele já chega transitivamente por
`spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test` e
`spring-boot-starter-validation-test`, e traz `junit-jupiter` + `mockito-core` +
`mockito-junit-jupiter`, que é o que `docs/04` exige. Adicionar seria redundante.

**Nomes conferidos para o Spring Boot 4.2.0** (start.spring.io + Maven Central):

| Nome usado | Observação |
|---|---|
| `spring-boot-starter-webmvc` | Substituto do antigo `spring-boot-starter-web` no Boot 4 |
| `spring-boot-starter-data-jpa` | Já presente; `docs/01` a listava como "prevista" |
| `spring-boot-starter-security` | Mesmo nome do Boot 3.x |
| `spring-boot-starter-test` | Continua existindo no 4.2.0, mas é transitivo aqui |

### 3.9 `README.md`

Criado com o cabeçalho do projeto e a seção que explica a remoção do teste
gerado. As demais seções (como rodar, endpoints, testes) serão preenchidas ao
final das specs, conforme o `CLAUDE.md`.

## 4. O que foi mantido de propósito

| Item | Motivo |
|---|---|
| `ddl-auto=update` | `docs/03` define para esta fase; Flyway só no futuro |
| MySQL 8.4 + `utf8mb4` | `docs/03` manda não alterar sem avisar; está coerente |
| `Dockerfile` multi-stage | Já conforme `docs/04` |
| `DB_HOST` e `DB_PORT` com padrão | Não são segredos e não estão na tabela de `docs/04`; úteis para rodar fora do Docker |
| `SERVER_PORT` com padrão 8080 | Não é segredo; só porta |
| `spring.config.import=optional:file:.env[.properties]` | Facilita rodar na máquina; dentro do container não tem efeito (o `.env` não entra na imagem) |
| `show-sql=true` e `open-in-view` | Nenhum doc exige mudança agora; o log só alerta sobre `open-in-view` |
| Starters de teste por fatia | Vieram do Initializr; atendem a `docs/04` via transitividade |

## 5. Pendências por spec

| Spec | Pendência |
|---|---|
| 001 | Adicionar `JWT_SECRET` ao `.env`, `.env.example` (`${JWT_SECRET:?...}` no compose) e `app.jwt.expiration-seconds=300`; criar `ClockConfig`, `SecurityConfig`, `JwtService`, filtro, entry point/handler, `GlobalExceptionHandler` e exceções; ao subir sem `JWT_SECRET`, a app deve falhar |
| 002 | `app.links.ttl-seconds=300` e `app.links.max-per-user=5`; entidade `ShortUrl`, gerador Base62 e service |
| 003 | `LinkExpiredException` (410) e uso do mesmo `ShortCodeGenerator`/service; redirect 302 |

Observação: ao adicionar `spring-boot-starter-security` antes da Spec 001, o
Spring Boot aplica a segurança padrão (HTTP Basic e todos os endpoints
protegidos). Isso é transitório e esperado até o `SecurityConfig` da Spec 001
definir as rotas públicas.

## 6. Checklist para projetos futuros

- [ ] Conferir no log do Hibernate a URL JDBC **efetiva**; ela mostra se os
      parâmetros foram realmente aplicados.
- [ ] Manter banco, JDBC e JVM no mesmo fuso (UTC) desde o início.
- [ ] Nunca usar quebra de linha em `spring.datasource.url` de `.properties`.
- [ ] Variável obrigatória: `${VAR:?mensagem}` no Compose, `${VAR}` no Spring.
- [ ] Healthcheck sem senha na linha de comando (`$$MYSQL_ROOT_PASSWORD`).
- [ ] Não deixar valor padrão para `DB_PASSWORD`, `DB_NAME`, `DB_USER`.
- [ ] Conferir nomes de starters do Boot 4 (ex.: `webmvc`, starters `-test`).
- [ ] Conferir se `spring-boot-starter-test` é transitivo antes de adicioná-lo.
- [ ] Versão explícita para bibliotecas fora do BOM do Boot (ex.: `java-jwt`).
- [ ] Não manter `@SpringBootTest` gerado se os docs exigem testes sem Spring.
- [ ] `.gitignore`: `.env`, `.env.*` e `!.env.example` (o `!` depois do `*`).
- [ ] Após cada mudança de configuração: `docker compose up -d --build api`,
      `docker compose ps`, `docker compose logs --tail=100 api` e o teste do banco
      do `CLAUDE.md`.

## 7. Verificação realizada

| Data | Verificação | Resultado |
|---|---|---|
| 2026-11-26 | URL JDBC e fuso | Parâmetros aplicados; MySQL e JVM em UTC |
| 2026-11-26 | Containers | `db` healthy; API subiu sem erro após as correções de config |
| 2026-11-26 | Build após o `pom.xml` | `./mvnw dependency:go-offline` + `package -DskipTests` executados dentro da imagem (comando terminou sem erro) |
| 2026-11-26 | Runtime após o `pom.xml` | Não verificado nesta rodada; esperado 401 padrão do Spring Security até a Spec 001 |
