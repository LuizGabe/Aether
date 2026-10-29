# Aether-finance-tracker

Projeto base em **Java 21 + Spring Boot** com ORM via **Spring Data JPA** e migrações **Flyway** para o schema Pluggy de finanças pessoais.

## Executar localmente

```bash
mvn spring-boot:run
```

Variáveis principais:

- `DB_URL` (default: `jdbc:postgresql://localhost:5432/aether`)
- `DB_USERNAME` (default: `postgres`)
- `DB_PASSWORD` (default: `postgres`)

## Workflows

- **Development CI** (`.github/workflows/development.yml`): roda testes em PR/push para `dev` e `main` com Java 21.
- **Deploy** (`.github/workflows/deploy.yml`): em push para `main`, builda o app e publica imagem no GHCR.


## Executar com Docker Compose (dev)

```bash
docker compose up --watch
```

Serviços disponíveis:

- App: `http://localhost:8080`
- PostgreSQL: `localhost:5432` (db `aether`, usuário `postgres`, senha `postgres`)

Nesse modo, alterações em `src/` ou no `pom.xml` disparam rebuild automático da imagem do app.

## Compilar o aplicativo de uso diário

Execute `pnpm build:app`. O executável fica em `C:\Dev\Aether\dist\Aether\Aether.exe`, junto da subpasta `resources`. Ele abre maximizado e pode ser fixado na barra de tarefas. Mantenha os arquivos dessa pasta juntos; esta entrega usa o Java já instalado no computador.

O comando gera uma versão release em um caminho fixo. O desenvolvimento continua com `pnpm dev`; editar o código não altera a cópia de uso diário até uma nova compilação explícita. Veja [a proposta de distribuição e licenciamento](docs/distribuicao-licenciamento.md).
