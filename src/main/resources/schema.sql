CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS workflow_failures (
    id UUID PRIMARY KEY,
    repository VARCHAR(255) NOT NULL,
    branch VARCHAR(255) NOT NULL,
    commit_sha VARCHAR(100) NOT NULL,
    pipeline_id VARCHAR(100) NOT NULL,
    raw_log TEXT NOT NULL,
    parsed_stacktrace TEXT,
    status VARCHAR(32) NOT NULL,
    failure_category VARCHAR(100),
    confidence_score DOUBLE PRECISION,
    root_cause_summary TEXT,
    remediation_steps TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_workflow_failures_status
    ON workflow_failures (status);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created
    ON outbox_events (status, created_at);

-- Spring AI PgVectorStore table (table-name: incident_runbooks). gen_random_uuid() is built into
-- PostgreSQL 13+, so the uuid-ossp extension is not required.
CREATE TABLE IF NOT EXISTS incident_runbooks (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS idx_incident_runbooks_embedding
    ON incident_runbooks USING hnsw (embedding vector_cosine_ops);
