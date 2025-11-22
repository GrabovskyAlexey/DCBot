-- changeset dungeoncrusherbot:30-create-debts-table
CREATE TABLE IF NOT EXISTS dc_bot.debts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES dc_bot.users(id),
    direction VARCHAR(32) NOT NULL,
    server_id INT NOT NULL REFERENCES dc_bot.servers(id),
    resource_type VARCHAR(32) NOT NULL,
    amount INT NOT NULL,
    counterparty_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE INDEX IF NOT EXISTS debts_user_idx ON dc_bot.debts(user_id);
