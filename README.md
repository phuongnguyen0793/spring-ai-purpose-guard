# sample-spring-ai

Kotlin Spring Boot application using **Spring AI** as the abstraction layer for two features:

1. **Semantic search over business purposes** — embeddings are persisted in **MySQL** and loaded into an **in-memory vector store** on startup for fast cosine-similarity search.
2. **Rule-based company name validation** — the `(industry_id, business_type, company_name, relation)` mapping is produced **offline by an LLM**, saved to a **versioned CSV**, then loaded incrementally into **MySQL** and used for data-driven validation.

## Stack

| Layer | Technology |
|-------|-----------|
| AI abstraction | Spring AI 1.0.0 (`EmbeddingModel`, `ChatClient`) |
| Embedding & chat | Ollama (`nomic-embed-text` / `llama3.2`) |
| Persistence | MySQL 8 (JDBC / `JdbcTemplate`) |
| Vector search | Custom in-memory index (cosine similarity), loaded from MySQL |
| Runtime | Kotlin + Spring Boot 3.4 / JVM 21 |

## Architecture

```
OFFLINE (LLM):  company_name_seed.csv ──(Ollama chat)──► company_name_rules.csv   (versioned)

STARTUP:        company_name_rules.csv ─(new versions only)─► MySQL company_name_rule + data_version
                MySQL business_purpose ─(load + embed any missing)─► InMemoryVectorStore

RUNTIME:        POST /api/validate-name  ─► format checks + MySQL relation lookup
                POST /api/search-purpose ─► embed query ─► in-memory cosine search
                POST /api/purposes       ─► persist to MySQL + index in memory
```

### Data model (MySQL)

| Table | Purpose |
|-------|---------|
| `business_purpose` | `purpose_text` + `embedding` (JSON array). Embeddings computed once, reused across restarts. |
| `company_name_rule` | Trained `(industry_id, business_type, company_name, relation, data_version)` rows. |
| `data_version` | Tracks which CSV data versions have been imported per dataset (incremental loading). |

Schema is created on startup from `src/main/resources/schema.sql` (`spring.sql.init.mode=always`).

## Prerequisites

- Docker & Docker Compose (for MySQL + Ollama)

## Run locally

```bash
# 1. Start MySQL + Ollama
docker compose up -d

# 2. Pull the models inside the Ollama container
docker exec ollama ollama pull nomic-embed-text
docker exec ollama ollama pull llama3.2

# 3. Start the application
#    - creates schema, seeds sample purposes, computes+persists embeddings,
#      loads them into the in-memory index, and imports data/company_name_rules.csv
./gradlew bootRun
```

### Local dev with Testcontainers (no docker compose needed)

```bash
./gradlew bootTestRun     # boots MySQL + Ollama via Testcontainers
```

## Offline LLM training (regenerate the rule CSV)

The rule dataset is produced offline. Provide seed rows in `data/company_name_seed.csv`
(`industry_id,business_type,company_name`), then run:

```bash
./gradlew bootRun --args="--app.training.enabled=true"
```

This asks the LLM to classify each seed row's `relation` (`CONSISTENT` / `INCONSISTENT` / `NEUTRAL`),
appends the results to `data/company_name_rules.csv` as a **new data version**, and exits.
On the next normal startup, `RuleDataLoader` imports only the newly-added version into MySQL.

## API

```
# Rule-based validation (data-driven)
POST /api/validate-name
{"companyName": "株式会社テックソリューション", "industryId": "IT", "businessType": "software"}

# Semantic search
POST /api/search-purpose
{"purpose": "health AI diagnostics", "topK": 5, "threshold": 0.5}

# Add + index a new purpose
POST /api/purposes
{"purpose": "Provide renewable energy solutions"}
```

### `validate-name` response

```json
{
  "companyName": "株式会社テックソリューション",
  "industryId": "IT",
  "businessType": "software",
  "valid": true,
  "reasons": [],
  "relation": "CONSISTENT",
  "matchedExample": "株式会社テックソリューション",
  "matchScore": 1.0,
  "rulesEvaluated": 3,
  "dataVersion": 1
}
```

## Configuration (`src/main/resources/application.yml`)

```yaml
spring.datasource.url: jdbc:mysql://localhost:3306/sample_ai?createDatabaseIfNotExist=true
spring.ai.ollama.embedding.options.model: nomic-embed-text
spring.ai.ollama.chat.options.model: llama3.2

app.search.default-top-k: 5
app.search.default-threshold: 0.5     # cosine similarity, 0..1 (higher = more similar)
app.rule.csv-path: data/company_name_rules.csv
app.training.enabled: false
app.training.seed-csv-path: data/company_name_seed.csv
app.training.output-csv-path: data/company_name_rules.csv
```

## Notes

- Similarity `score` is cosine similarity in `[0, 1]` (higher = more similar).
- Embeddings are computed once and persisted; restarts reuse them (only missing ones are recomputed).
- Rule import is **incremental and versioned** — re-running with an unchanged CSV imports nothing.
- Production hardening (auth, rate-limiting, retries, migrations tool, secure secrets) is required beyond this MVP.
```

