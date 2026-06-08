# 📑 Guia de Comandos do MongoDB (Via Docker)

Este guia contém os comandos de pista essenciais para consultar e gerenciar os dados da API Encurtadora de Links diretamente pelo Terminal do IntelliJ.

> ⚠️ **Nota de Voo:** Como o Spring Boot aplicou as configurações padrão na inicialização local, as consultas devem ser direcionadas estritamente para a base de dados chamada **`test`**.

---

### 🔍 1. Listar todos os links criados (Histórico)
Exibe a lista completa de documentos JSON contendo Nome, E-mail, URL Original, Código Curto e as datas de criação e expiração.
```bash
docker exec url_shortener_mongodb mongosh test --eval "db.url_mappings.find()"
```

### 🧹 2. Limpar a tela e consultar (Visualização Limpa)
Executa a limpeza dos logs antigos do terminal e cospe a lista atualizada de dados na sequência.
*   **No Linux / Mac:**
    ```bash
    clear && docker exec url_shortener_mongodb mongosh test --eval "db.url_mappings.find()"
    ```
*   **No Windows:**
    ```bash
    cls && docker exec url_shortener_mongodb mongosh test --eval "db.url_mappings.find()"
    ```

### 🔢 3. Contar total de registros no banco
Retorna de forma direta o número exato de links salvos no histórico até o momento.
```bash
docker exec url_shortener_mongodb mongosh test --eval "db.url_mappings.countDocuments()"
```

### 🗑️ 4. Resetar a tabela (Zerar para novos testes)
Deleta permanentemente todos os dados da tabela caso você precise zerar os contadores e o histórico para iniciar uma nova bateria de testes do absoluto zero.
```bash
docker exec url_shortener_mongodb mongosh test --eval "db.url_mappings.drop()"
```
