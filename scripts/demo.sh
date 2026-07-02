#!/usr/bin/env bash
# One-command demo: start dependencies, boot the app, hit validate + search APIs.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BASE_URL="${BASE_URL:-http://localhost:8080}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-300}"

cleanup() {
  if [[ -n "${APP_PID:-}" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    echo ""
    echo "Stopping application (pid $APP_PID)..."
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

echo "==> Starting MySQL + Ollama (docker compose)..."
docker compose up -d

echo "==> Pulling Ollama models (skipped if already present)..."
docker exec ollama ollama pull nomic-embed-text
docker exec ollama ollama pull llama3.2

echo "==> Starting Spring Boot (./gradlew bootRun)..."
./gradlew --quiet bootRun &
APP_PID=$!

echo "==> Waiting for $BASE_URL (up to ${MAX_WAIT_SECONDS}s; first boot embeds sample data)..."
deadline=$((SECONDS + MAX_WAIT_SECONDS))
until curl -sf -o /dev/null -X POST "$BASE_URL/api/search-purpose" \
  -H "Content-Type: application/json" \
  -d '{"purpose":"health AI","topK":1,"threshold":0.0}'; do
  if (( SECONDS >= deadline )); then
    echo "Timed out waiting for the app. Check ./gradlew bootRun logs."
    exit 1
  fi
  sleep 3
done

echo ""
echo "==> Demo 1: validate company name (IT / software)"
curl -s -X POST "$BASE_URL/api/validate-name" \
  -H "Content-Type: application/json" \
  -d '{"companyName":"株式会社テックソリューション","industryId":"IT","businessType":"software"}' \
  | (command -v jq >/dev/null && jq . || cat)

echo ""
echo "==> Demo 2: semantic purpose search"
curl -s -X POST "$BASE_URL/api/search-purpose" \
  -H "Content-Type: application/json" \
  -d '{"purpose":"AI healthcare diagnostics"}' \
  | (command -v jq >/dev/null && jq . || cat)

echo ""
echo "Done. More examples: http/api.http and doc/API_TESTING.md"
