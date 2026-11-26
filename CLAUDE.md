# Contexto do Projeto: URL Shortener API

API REST para encurtar links, com login JWT. Projeto de estudo para aprender SDD (Spec-Driven Development).
Stack: Java 25, Spring Boot 4.2, MySQL, Docker.
Repositório: https://github.com/fabricio948/url-shortener-api.git

## Regra Principal
Nunca invente nada. Não crie arquivos, campos, endpoints, dependências ou configurações que não estejam em `docs/` ou em `docs/specs/`. Se faltar informação ou houver conflito entre arquivos, PARE e me pergunte.

## Ambiente: Java NÃO está instalado no computador
Java 25 e MySQL rodam só dentro do Docker. Nunca rode `./mvnw` direto no terminal e nunca instale Java no sistema.

Antes de usar qualquer comando abaixo, leia o `docker-compose.yml` e use os nomes reais dos serviços e containers.

| Tarefa | Comando |
|---|---|
| Ver containers | `docker compose ps` |
| Subir só o banco | `docker compose up -d db` |
| Subir banco e API (recompilando) | `docker compose up --build -d` |
| Recompilar só a API | `docker compose up -d --build api` |
| Ver logs da API | `docker compose logs --tail=100 api` |
| Testar o banco (sem mostrar a senha) | `docker compose exec db sh -c 'mysqladmin ping -h localhost -u root -p"$MYSQL_ROOT_PASSWORD"'` |
| Entrar no MySQL | `docker compose exec db sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD"'` |
| Compilar | `docker run --rm -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -w /app eclipse-temurin:25-jdk ./mvnw test-compile` |
| Rodar testes | `docker run --rm -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -w /app eclipse-temurin:25-jdk ./mvnw test` |
| Parar tudo (mantém dados) | `docker compose down` |

- Nunca use `docker compose down -v` sem minha autorização (apaga os dados do banco).
- Se aparecer erro de permissão na pasta `target/` (dono root), PARE e me avise. Nunca use `sudo` nem `chmod 777`.

## Segredos
- Senha do banco e `JWT_SECRET` ficam só no `.env`. Sem valor padrão no código, no `docker-compose.yml` ou no `application.properties`.
- Nunca mostre, copie ou comite o conteúdo do `.env`. Mantenha o `.env.example` sem valores reais.

## Idioma e Padrão de Código
- Código-fonte (classes, métodos, variáveis, endpoints, tabelas): inglês.
- Documentação (.md), commits e comentários: português (PT-BR). Comentários curtos, só para regra de negócio complexa.

## Leitura obrigatória (SDD)
Antes de gerar ou alterar código, leia:
- `@docs/01-stack-e-arquitetura.md`
- `@docs/02-api-e-erros.md`
- `@docs/03-database.md`
- `@docs/04-qualidade-e-infra.md`
Implemente apenas a spec pedida em `@docs/specs/`, uma de cada vez.

## Dependências
- O `pom.xml` já tem: Web, Validation e MySQL Driver. Leia o `pom.xml` real antes de qualquer coisa.
- Compare com o que `docs/` exige e liste o que falta.
- Para CADA dependência faltante, explique: o que é, por que este projeto precisa dela e em qual spec será usada.
- Só altere o `pom.xml` depois do meu "ok". Confira os nomes no start.spring.io, pois o Spring Boot 4 mudou nomes de starters em relação ao 3.x.
- Se uma dependência for desnecessária, não adicione.

## Fluxo inicial (nesta ordem, parando ao final de cada etapa)
1. Ler `CLAUDE.md`, `docs/` e o projeto real. Apontar diferenças, sem alterar nada.
2. Verificar containers: `docker compose ps`, banco saudável, API subindo (logs sem erro).
3. Analisar dependências (seção acima) e esperar meu "ok".
4. Git (só depois de confirmar que `.gitignore` bloqueia `.env` e `target/`):
   - `git init` se não existir, branch `main`.
   - Remoto `origin` = URL do repositório acima.
   - Rodar `git ls-remote origin`. Se o remoto não estiver vazio, PARE e me avise. Nunca use `--force`.
   - Rodar `git status` e me mostrar que `.env` não está na lista antes do primeiro commit.
5. Primeiro commit: `chore: estrutura inicial do projeto`.

## Git
- Trabalhe direto na branch `main`.
- Commits pequenos, em português, Conventional Commits: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`.
- Um commit por spec concluída, só com os testes passando.
- NUNCA rode `git push`. Depois de cada commit, me avise com: "Agora rode no seu terminal: `git push -u origin main`" (o `-u` só no primeiro push; nos próximos, `git push`).
- Explique em uma frase o que o push faz, pois estou aprendendo.

## README.md
Ao final, o Claude Code preenche o `README.md` em PT-BR, claro e curto, com:
- O que é o projeto e a stack.
- Como criar o `.env` a partir do `.env.example`.
- Como subir com Docker Compose.
- Tabela de endpoints com um exemplo `curl` de cada.
- Como rodar os testes (comando via Docker).

## Quando parar e perguntar
Conflito entre arquivos, erro de configuração, dependência faltando, remoto do GitHub não vazio, erro de permissão ou qualquer dúvida sobre a spec.