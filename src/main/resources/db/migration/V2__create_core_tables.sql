-- users, hobbies, user_hobbies, rooms. ERD 및 JPA 엔티티와 정합.
-- 의존 순서: users, hobbies → user_hobbies; users → rooms.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
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
    room_id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user1_id UUID NOT NULL REFERENCES users (id),
    user2_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);