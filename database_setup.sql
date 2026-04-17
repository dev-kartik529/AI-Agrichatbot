-- ============================================
-- AgriChat Database Setup
-- Run this ONCE to create the database
-- ============================================

-- 1. Create database (run as superuser)
CREATE DATABASE agrichat_db;

-- 2. Connect to the new database
\c agrichat_db;

-- Note: The tables below are auto-created by Spring Boot (ddl-auto=update)
-- But you can also create them manually here for reference:

-- Chat Sessions Table
CREATE TABLE IF NOT EXISTS chat_sessions (
    id          BIGSERIAL PRIMARY KEY,
    session_id  VARCHAR(255) UNIQUE NOT NULL,
    user_name   VARCHAR(100),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    last_active TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Chat Messages Table
CREATE TABLE IF NOT EXISTS chat_messages (
    id               BIGSERIAL PRIMARY KEY,
    session_id       VARCHAR(255) NOT NULL REFERENCES chat_sessions(session_id),
    role             VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content          TEXT NOT NULL,
    timestamp        TIMESTAMP NOT NULL DEFAULT NOW(),
    category         VARCHAR(50),
    feedback_rating  INT CHECK (feedback_rating BETWEEN 1 AND 5)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_messages_session_id ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp  ON chat_messages(timestamp);
CREATE INDEX IF NOT EXISTS idx_sessions_last_active ON chat_sessions(last_active);

-- ============================================
-- Useful queries for development/testing
-- ============================================

-- See all sessions:
-- SELECT * FROM chat_sessions ORDER BY last_active DESC;

-- See messages for a session:
-- SELECT role, content, timestamp FROM chat_messages WHERE session_id = '<your-id>' ORDER BY timestamp;

-- Count messages by category:
-- SELECT category, COUNT(*) FROM chat_messages GROUP BY category;
