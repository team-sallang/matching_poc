-- V1 + V2 병합. match_queue(V1) → users, hobbies, user_hobbies, rooms(V2). IF NOT EXISTS로 재실행 시 무해.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- match_queue: FK 없음. GIN 인덱스로 hobby_ids && 연산 지원.
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
CREATE INDEX IF NOT EXISTS idx_match_queue_hobby_ids_gin ON match_queue USING GIN (hobby_ids);

-- users, hobbies, user_hobbies, rooms. 의존: users, hobbies → user_hobbies; users → rooms.
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nickname VARCHAR(50) UNIQUE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    birth_date DATE NOT NULL,
    region VARCHAR(50) NOT NULL,
    total_score INT NOT NULL DEFAULT 0,
    tier VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hobbies (
    id SERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    subcategory VARCHAR(50),
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_hobbies (
    user_hobby_id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    hobby_id INT NOT NULL REFERENCES hobbies (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    UNIQUE (user_id, hobby_id)
);

CREATE TABLE IF NOT EXISTS rooms (
    room_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL REFERENCES users (id),
    user2_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);
