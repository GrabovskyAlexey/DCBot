--liquibase formatted sql

--changeset dc-bot:cleanup_verification
ALTER TABLE IF EXISTS user_state
    DROP COLUMN IF EXISTS verification_request_id;

DROP TABLE IF EXISTS verification_request;
