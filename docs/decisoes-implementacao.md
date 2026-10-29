# Decisoes de implementacao

## 2026-10-27 - Recuperacao de conexoes Pluggy

- Substituida a consulta ao host interno `my-api.pluggy.ai` pela rota publica `GET https://api.pluggy.ai/v2/items`, autenticada por `X-API-KEY`, com paginacao por cursor.
- A listagem depende de habilitacao da conta. Quando indisponivel, a interface explica a restricao; nao tenta contornar a autorizacao.
- IDs recebidos em `data.items` no erro `ITEM_USER_ALREADY_EXISTS` sao validados na API, registrados localmente e oferecidos para sincronizacao. Uma unica conexao recuperada pode ser sincronizada automaticamente.
- O evento `ITEM_RESPONSE` registra a conexao antes do sucesso final; o widget pode ser reaberto em modo de atualizacao (`updateItem`). Nem todo erro do widget necessariamente expoe IDs ao SDK.
- Erros Pluggy durante uma sincronizacao conhecida preservam o job e seu diagnostico; `ITEM_NOT_FOUND` marca a referencia local como ausente sem apagar dados financeiros.
- Credenciais alteradas invalidam o uso do token anterior. Consultas que recebem HTTP 401 renovam a API key uma vez.
- Removido `pageSize` de `/v2/transactions`; a sincronizacao segue a query `next` sem decodificar/recriar o cursor e detecta paginas repetidas ou limite excedido.
- O painel usa contagens do banco via `/api/summary`; a lista continua limitada as 500 transacoes mais recentes e informa essa limitacao. Referencias marcadas `MISSING` nao contam como conexoes ativas.
- Nomes definidos para conexoes sao preservados durante a sincronizacao; o nome generico do conector MeuPluggy nao substitui o nome do banco. A importacao por Item ID aceita atualizar explicitamente esse nome.
- Fontes: https://docs.pluggy.ai/en/docs/connections/item, https://docs.pluggy.ai/en/docs/connect-widget/updating-item, https://v2.docs.pluggy.ai/en/reference/transaction/transactions-list-by-cursor.

## 2026-09-11 - Fundacao do monorepo

- O projeto foi reorganizado para `apps/api` e `apps/desktop`, seguindo a documentacao tecnica.
- O backend passou de PostgreSQL para SQLite, com porta local padrao `38472` e bind em `127.0.0.1`.
- O schema inicial foi reduzido ao essencial para a primeira milestone: instituicoes, contas, transacoes, categorias, sync jobs e settings nao sensiveis.
- A tabela `institutions` foi usada como nome inicial para a entidade pedida na meta pratica. A documentacao tambem usa o conceito `connections`; essa convergencia deve ser resolvida quando a Pluggy for implementada.
- O desktop Tauri tenta iniciar o backend local como processo filho. Em dev usa Maven quando o JAR ainda nao existe; em build usa o JAR gerado.
