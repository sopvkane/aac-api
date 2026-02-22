# AAC API (aac-api)

Spring Boot (Java 17) API for the AAC project.

## Prerequisites
- Java 17
- Docker + Docker Compose

## Run locally (Postgres + API)

### 0) Start Postgres (required)
```bash
docker compose up -d
docker compose ps
docker exec -it aac-postgres pg_isready -U app -d aac
