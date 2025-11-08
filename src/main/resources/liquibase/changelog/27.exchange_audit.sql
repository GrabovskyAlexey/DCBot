--liquibase formatted sql

--changeset dungeoncrusherbot:create_exchange_audit
CREATE TABLE IF NOT EXISTS dc_bot.exchange_audit
(
    id               BIGSERIAL PRIMARY KEY,
    event_type       VARCHAR(64)                NOT NULL,
    user_id          BIGINT                     NOT NULL,
    request_type     VARCHAR(32),
    source_server_id INT,
    target_server_id INT,
    matches_count    INT,
    contact_user_id  BIGINT,
    metadata         JSONB                      NOT NULL DEFAULT '{}'::jsonb,
    created_at       TIMESTAMPTZ                NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS exchange_audit_created_at_idx
    ON dc_bot.exchange_audit (created_at);

CREATE INDEX IF NOT EXISTS exchange_audit_event_type_idx
    ON dc_bot.exchange_audit (event_type);
