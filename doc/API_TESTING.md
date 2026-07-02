# API Testing Guide

Base URL: `http://localhost:8080`

---

## 1. POST /api/validate-name (rule-based, data-driven)

Validates a company name against baseline format rules **and** the trained
`(industry_id, business_type, company_name, relation)` data loaded from CSV into MySQL.

### Request
```json
{
  "companyName": "株式会社テックソリューション",
  "industryId": "IT",
  "businessType": "software"
}
```

### Response
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

- `relation`: `CONSISTENT` / `INCONSISTENT` / `NEUTRAL` — derived from the best-matching trained rule.
- `matchScore`: 1.0 for an exact (normalized) name match, otherwise token-overlap (Jaccard) similarity.
- `dataVersion`: the latest rule data version currently loaded in MySQL.

### Examples
```bash
# Consistent name for the industry/business type
curl -X POST http://localhost:8080/api/validate-name \
  -H "Content-Type: application/json" \
  -d '{"companyName":"Tech Solutions Inc","industryId":"IT","businessType":"software"}'

# Name that conflicts with the domain -> relation INCONSISTENT, valid=false
curl -X POST http://localhost:8080/api/validate-name \
  -H "Content-Type: application/json" \
  -d '{"companyName":"ABC Bakery","industryId":"IT","businessType":"software"}'
```

---

## 2. POST /api/search-purpose (semantic search)

Embeds the query via Ollama and searches the in-memory vector store (loaded from MySQL).

### Request
```json
{
  "purpose": "AI healthcare diagnostics",
  "topK": 5,
  "threshold": 0.5
}
```
`topK` and `threshold` are optional (defaults: `app.search.default-top-k`, `app.search.default-threshold`).

### Response
```json
{
  "query": "AI healthcare diagnostics",
  "topK": 5,
  "threshold": 0.5,
  "count": 1,
  "results": [
    { "id": 2, "purposeText": "Develop AI-powered healthcare diagnostics", "score": 0.87 }
  ]
}
```

`score` is cosine similarity in `[0, 1]` — **higher means more similar**.

### Examples
```bash
curl -X POST http://localhost:8080/api/search-purpose \
  -H "Content-Type: application/json" \
  -d '{"purpose":"payments platform"}'

curl -X POST http://localhost:8080/api/search-purpose \
  -H "Content-Type: application/json" \
  -d '{"purpose":"payments platform","topK":3,"threshold":0.7}'
```

---

## 3. POST /api/purposes (add + index)

Persists a new purpose to MySQL, computes its embedding, and adds it to the in-memory index
so it is immediately searchable.

```bash
curl -X POST http://localhost:8080/api/purposes \
  -H "Content-Type: application/json" \
  -d '{"purpose":"Provide renewable energy solutions"}'
```

Response:
```json
{ "id": 6, "purposeText": "Provide renewable energy solutions", "score": 1.0 }
```

---

## Threshold tuning (cosine similarity)

| threshold | Behavior |
|-----------|----------|
| 0.8 - 1.0 | Only near-identical meaning |
| 0.6 - 0.8 | Closely related |
| 0.4 - 0.6 | Related (balanced default) |
| 0.0 - 0.4 | Broad / loosely related |
