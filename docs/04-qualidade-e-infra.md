# Qualidade, Testes, Docker e Git

## Testes automatizados
- Ferramentas: **JUnit 5** e **Mockito** (`@ExtendWith(MockitoExtension.class)`).
- Escopo: testes unitários da camada **Service**. É lá que ficam as regras de negócio.
- Testes **não** usam banco, Docker nem Spring (sem `@SpringBootTest`). Repository e dependências são mocks.
- Testes de integração e de Controller **não** fazem parte desta fase, a menos que uma spec peça.
- Estrutura dos testes: **Given / When / Then**, com um comentário curto em português por bloco.
- Nome do teste descreve o comportamento, em inglês (ex: `shouldThrowWhenActiveLimitIsReached`).
- Espelhar os pacotes: `ShortUrlService` em `src/main/...` tem `ShortUrlServiceTest` em `src/test/...`, no mesmo pacote.
- Cada regra de negócio nova precisa de pelo menos: 1 teste do caminho feliz e 1 teste por exceção que ela lança.

### Tempo nos testes
- Usar `Clock.fixed(...)` em UTC. Nunca `Thread.sleep` para esperar o link expirar.
- Para simular a passagem de tempo, criar o Service com outro `Clock` fixo (ex: 5 minutos e 1 segundo depois).

### O que deve ser testado (resumo; a spec manda no detalhe)
| Área | Casos mínimos |
|---|---|
| Auth/Usuários | Registro com sucesso, e-mail duplicado (409), senha salva com hash, login inválido |
| Criar link | Sucesso, alias duplicado (409), limite de 5 links ativos (422), expirados não contam no limite, `expiresAt` = agora + prazo |
| Listar/Excluir | Lista só do dono, exclusão do dono, link inexistente (404), link de outro usuário (403) |
| Redirecionar | Sucesso e contagem de clique, código inexistente (404), link expirado (410) |
| Estatísticas | Sucesso para o dono, link de outro usuário (403), link inexistente (404) |

### Regra de ouro
- Nenhum commit de feature com teste falhando.
- Nunca apagar ou enfraquecer um teste só para ele passar. Se um teste parece errado, parar e perguntar.

## Como rodar (Java só existe no Docker)
Nunca usar `./mvnw` direto no terminal. Usar o comando do `CLAUDE.md`:
```bash
docker run --rm -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -w /app eclipse-temurin:25-jdk ./mvnw test
```
Se aparecer erro de permissão em `target/` ou em `~/.m2`, parar e avisar. Nunca usar `sudo` nem `chmod 777`.

## Docker e infraestrutura
- API e MySQL em **containers separados**, no mesmo `docker-compose.yml`.
- A API só inicia depois que o MySQL estiver saudável (`healthcheck` + `depends_on` com `condition: service_healthy`).
- O `Dockerfile` da API é multi-stage (compila com JDK 25, roda com JRE 25). Não alterar sem avisar.
- Dados do MySQL ficam em volume nomeado. `docker compose down -v` apaga esse volume: nunca rodar sem minha autorização.
- Não alterar `docker-compose.yml`, `Dockerfile` ou `.env` sem mostrar a mudança e esperar o "ok".

## Variáveis de ambiente e segredos

| Variável | Segredo? | Valor padrão? |
|---|---|---|
| `DB_NAME` | Não | Não, vem do `.env` |
| `DB_USER` | Não | Não, vem do `.env` |
| `DB_PASSWORD` | **Sim** | **Nunca** |
| `JWT_SECRET` | **Sim** | **Nunca** |

- `DB_PASSWORD` e `JWT_SECRET` são **obrigatórios**. Se faltarem, o sistema deve falhar ao iniciar, não usar um valor escondido no código.
- No `docker-compose.yml`, usar a forma obrigatória `${DB_PASSWORD:?defina DB_PASSWORD no .env}` (e igual para `JWT_SECRET`).
- O healthcheck do MySQL não pode ter a senha escrita na linha de comando. Usar a variável que já existe dentro do container (`$$MYSQL_ROOT_PASSWORD`).
- `JWT_SECRET` com pelo menos 32 caracteres aleatórios. Gerar com: `openssl rand -base64 48`.
- Configurações que **não** são segredo (prazo e limite dos links) ficam em `application.properties` com valor padrão: `app.links.ttl-seconds=300` e `app.links.max-per-user=5`.
- Nunca imprimir, logar ou comitar o conteúdo do `.env`. Nunca colocar senha em log, exceção, README ou mensagem de commit.
- `.env.example` existe no Git, com nomes das variáveis e **valores de exemplo falsos**.
- O `.gitignore` precisa bloquear, no mínimo: `.env`, `.env.*` (exceto `.env.example`) e `target/`.

## Git (Conventional Commits)
- Trabalhar direto na branch `main` nesta fase.
- Commits pequenos, em português, no formato `tipo: descrição curta em minúsculas`.

| Prefixo | Uso |
|---|---|
| `feat:` | Nova funcionalidade ou endpoint |
| `fix:` | Correção de bug |
| `test:` | Testes |
| `refactor:` | Mudança interna sem mudar comportamento |
| `docs:` | Documentação (README, docs/) |
| `chore:` | Configuração, Docker, dependências |

- Um commit por spec concluída, somente com testes passando.
- Antes de todo commit: rodar `git status` e confirmar que `.env` e `target/` **não** estão na lista.
- **Nunca** rodar `git push`, `git push --force`, `git reset --hard` ou apagar branch. Depois do commit, avisar o usuário para rodar o push no terminal dele.
- Antes do primeiro push: `git ls-remote origin`. Se o remoto não estiver vazio, parar e avisar.

## Checklist antes de cada commit
- [ ] `./mvnw test` (via Docker) passou.
- [ ] `git status` sem `.env` e sem `target/`.
- [ ] Nenhuma senha, token ou segredo em arquivo, log ou mensagem.
- [ ] Nada criado fora do que a spec pede.
- [ ] Nenhuma dependência nova sem aprovação.
- [ ] Mensagem de commit no padrão, em português.