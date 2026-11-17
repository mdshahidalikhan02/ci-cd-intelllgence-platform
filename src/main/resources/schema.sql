-- Enable vector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Workflows Table (Main Entity)
CREATE TABLE IF NOT EXISTS workflow_failures (
    id VARCHAR(64) PRIMARY KEY,
    repo_name VARCHAR(128) NOT NULL,
    run_attempt INT NOT NULL,
    raw_log TEXT,
    parsed_stacktrace TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Transactional Outbox Table
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_events(status, created_at);

-- Knowledge Base Vector Store Table for RAG
CREATE TABLE IF NOT EXISTS incident_runbooks (
    id VARCHAR(36) PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS idx_runbooks_embedding ON incident_runbooks 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
