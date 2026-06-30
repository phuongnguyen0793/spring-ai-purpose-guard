# sample-spring-ai

Kotlin Spring Boot application using **Spring AI** as the abstraction layer for:
- Japanese company name rule-based validation
- Semantic search over business purposes using Spring AI `VectorStore`

## Stack

| Layer | Technology |
|-------|-----------|
| AI abstraction | Spring AI 1.0.0 |
| Embedding & chat | Ollama (`nomic-embed-text` / `llama3.2`) |
| Vector store | OpenSearch 2.x |
| Runtime | Kotlin + Spring Boot 3.4 / JVM 21 |

## Prerequisites

- Docker & Docker Compose
- Ollama running (included in Docker Compose) with the embedding model pulled

## Run locally

```bash
# 1. Start OpenSearch + Ollama
docker compose up -d

# 2. Pull the embedding model inside the Ollama container
docker exec ollama ollama pull nomic-embed-text

# 3. (Optional) Pull a chat model
docker exec ollama ollama pull llama3.2

# 4. Start the application — it seeds sample purposes into OpenSearch on first run
./gradlew bootRun
```

## API

```
POST /api/validate-name
{"companyName": "株式会社サンプル"}

POST /api/search-purpose
{"purpose": "health AI diagnostics", "topK": 5, "threshold": 0.5}
```

## Configuration

All AI settings are in `src/main/resources/application.yml`:

```yaml
spring.ai.ollama.base-url: http://localhost:11434
spring.ai.ollama.embedding.options.model: nomic-embed-text
spring.ai.ollama.chat.options.model: llama3.2
spring.ai.vectorstore.opensearch.uris: http://localhost:9200
spring.ai.vectorstore.opensearch.index-name: purposes
spring.ai.vectorstore.opensearch.initialize-schema: true
```

## Notes

- The vector store index is created automatically on first startup (`initialize-schema: true`).
- Sample purposes are seeded once on startup; subsequent restarts detect existing data and skip re-seeding.
- Similarity scores from OpenSearch range 0–1 (higher = more similar), unlike the previous pgvector distance (lower = closer).
- Production hardening (auth, rate-limiting, retries, secure secrets) is required beyond this MVP.
