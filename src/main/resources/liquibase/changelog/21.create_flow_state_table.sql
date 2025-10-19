-- changeset dungeoncrusherbot:21-create-flow-state-table
CREATE TABLE IF NOT EXISTS dc_bot.flow_state (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flow_key VARCHAR(64) NOT NULL,
    step_key VARCHAR(64) NOT NULL,
    payload JSONB NULL,
    message_bindings JSONB NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS flow_state_user_key_uindex
    ON dc_bot.flow_state (user_id, flow_key);

UPDATE dc_bot.user_state
SET state = 'START'
WHERE state IN ('SUBSCRIBE', 'UPDATE_SUBSCRIBE');

UPDATE dc_bot.user_state
SET prev_state = NULL
WHERE prev_state IN ('SUBSCRIBE', 'UPDATE_SUBSCRIBE');
