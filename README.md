# CI/CD Failure Intelligence Platform

Event-driven, AI-powered CI/CD root-cause analysis platform built with **Java 21**, **Spring Boot 4.1.2**, **Spring AI 2.0.1**, **Apache Kafka 4.2.0**, and **PostgreSQL 16 (`pgvector`)**.

## Architecture Overview
1. **Webhook Ingestion (`POST /api/v1/webhooks/failures`)**: Atomically persists the `WorkflowFailure` and an `OutboxEvent` inside a single PostgreSQL transaction.
2. **Transactional Outbox Relay (`OutboxPublisherScheduler`)**: Polls `PENDING` outbox records and publishes events to Kafka (`cicd.failure.events`) with retry and dead-letter handling.
3. **RAG + LLM Diagnostic Pipeline (`FailureEventConsumer` -> `RunbookRetrievalService` -> `DiagnosticEngine`)**: Retrieves matching historical incident runbooks via `pgvector` cosine similarity search and generates structured `FailureReport` diagnostics using Spring AI's `BeanOutputConverter`.

## Quick Start (Windows PowerShell)

```powershell
# 1. Start PostgreSQL (port 5433) and Kafka (port 9092)
docker compose up -d

# 2. (Optional) Export your OpenAI API key for live embeddings & LLM diagnostics
$env:OPENAI_API_KEY = "sk-your-key"

# 3. Compile and run the application
mvn clean spring-boot:run
```

## Sample Runbooks (RAG)
On first startup `RunbookSeeder` indexes four sample runbooks into `incident_runbooks` (needs a real `OPENAI_API_KEY` for embeddings; with the mock key it logs a warning and continues). Disable with `app.runbooks.seed-on-startup=false`. Add your own:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/runbooks" -Method Post -ContentType "application/json" `
  -Body '{"title":"Disk full on runner","content":"Symptom: No space left on device. Fix: prune docker images and caches."}'
```

## Tests
`mvn clean package` runs the unit tests; the Testcontainers end-to-end test (`CicdIntelligenceIntegrationTest`) runs when Docker is available and is skipped otherwise.

## Verify End-to-End Flow

```powershell
# Ingest a CI/CD failure webhook
$res = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/webhooks/failures" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{
    "repository": "platform/payment-service",
    "branch": "main",
    "commitSha": "a1b2c3d4",
    "pipelineId": "run-9042",
    "rawLog": "Caused by: org.postgresql.util.PSQLException: FATAL: remaining connection slots are reserved for roles with the SUPERUSER attribute"
  }'

# Query the analyzed failure record
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/failures/$($res.failureId)"
```
