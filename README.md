# URL Shortener API

API REST para encurtar links, com autenticação JWT. Projeto de estudo de SDD
(Spec-Driven Development).

Stack: Java 25, Spring Boot 4.2, MySQL 8.4 e Docker.

> As seções de como rodar, variáveis de ambiente e endpoints serão preenchidas ao
> final das specs em `docs/specs/`, conforme o `CLAUDE.md`.

## Testes automatizados

O teste `UrlShortenerApiApplicationTests` gerado pelo Spring Initializr foi
removido. Ele usava `@SpringBootTest`, que sobe o contexto do Spring (e tentaria
conectar ao banco). O `docs/04-qualidade-e-infra.md` define que, nesta fase, os
testes são **unitários da camada Service**, com JUnit 5 e Mockito, **sem Spring,
sem banco e sem Docker**. As specs seguintes criam seus testes em `src/test/...`,
espelhando o pacote da classe testada.
