# sample-spring-ai (MVP)

Local Kotlin Spring Boot MVP demonstrating:
- Japanese company name rule-based validation
- Semantic search over business purposes using pgvector + Ollama

Requirements:
- Docker & Docker Compose
- Ollama local server available at http://localhost:11434 (Docker compose includes an image)

Run locally:
1. docker compose up -d
2. ./gradlew bootRun
3. POST /api/validate-name {"companyName":"株式会社サンプル"}
   POST /api/search-purpose {"purpose":"health AI diagnostics"}

Notes:
- Ollama API shapes may differ; adjust OllamaClient endpoints and model names to match your Ollama version.
- Production hardening (auth, rate-limiting, retries, proper embedding dimensions, secure secrets) is required beyond this MVP.
