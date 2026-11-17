# CI/CD Failure Intelligence Platform

An AI-powered diagnostic engine built with Java 21, Spring Boot 3, Spring AI, PostgreSQL (pgvector), Apache Kafka, and GitHub Actions integration.

## Getting Started

1. Start infrastructure services:
   ```bash
   docker compose up -d
   ```

2. Build and run the Spring Boot service:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

3. Trigger a sample webhook test:
   ```bash
   curl -X POST http://localhost:8080/api/v1/webhooks/github \
     -H "Content-Type: application/json" \
     -d '{
       "action": "completed",
       "workflow_run": {
         "id": "run-101",
         "name": "Build and Test",
         "status": "completed",
         "conclusion": "failure",
         "run_attempt": 1,
         "html_url": "https://github.com/example/repo/actions/runs/101",
         "repository": { "full_name": "example/repo" },
         "head_commit": { "id": "abc123", "message": "Fix build issue" }
       }
     }'
   ```
