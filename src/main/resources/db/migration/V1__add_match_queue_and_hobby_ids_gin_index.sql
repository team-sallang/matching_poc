-- match_queue: ensure table exists (for fresh Supabase DB), then add GIN index for hobby_ids && operator.
-- PostgreSQL: default GIN operator class for arrays supports <@, @>, =, && (indexes-types, GIN).
CREATE TABLE IF NOT EXISTS match_queue (
    queue_id   BIGSERIAL PRIMARY KEY,
    user_id    UUID NOT NULL UNIQUE,
    status     VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    hobby_ids  INTEGER[],
    tier       VARCHAR(20),
    location   VARCHAR(50),
    birth_year INT,
    gender     VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_match_queue_status ON match_queue (status);
CREATE INDEX IF NOT EXISTS idx_match_queue_created_at ON match_queue (created_at);
-- GIN on integer[]: default opclass supports && (overlap). intarray.gin__int_ops needs CREATE EXTENSION intarray.
CREATE INDEX IF NOT EXISTS idx_match_queue_hobby_ids_gin ON match_queue USING GIN (hobby_ids);
