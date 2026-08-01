# Aether-finance-tracker

Projeto base em **Java 17 + Spring Boot** com ORM via **Spring Data JPA** e migrações **Flyway** para o schema Pluggy de finanças pessoais.

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
docker compose up --build
```

Serviços disponíveis:

- App: `http://localhost:8080`
- PostgreSQL: `localhost:5432` (db `aether`, usuário `postgres`, senha `postgres`)
