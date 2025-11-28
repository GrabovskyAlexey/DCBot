--liquibase formatted sql

--changeset dungeoncrusherbot:create_resource_server_state_table
CREATE TABLE IF NOT EXISTS dc_bot.resource_server_state
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES dc_bot.users (id) ON DELETE CASCADE,
    server_id        INT         NOT NULL REFERENCES dc_bot.servers (id),
    exchange_label   TEXT,
    exchange_username TEXT,
    exchange_user_id BIGINT REFERENCES dc_bot.users (id),
    draador_count    INT         NOT NULL DEFAULT 0,
    void_count       INT         NOT NULL DEFAULT 0,
    cb_count         INT         NOT NULL DEFAULT 0,
    balance          INT         NOT NULL DEFAULT 0,
    notify_disable   BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_resource_server_state UNIQUE (user_id, server_id)
);

CREATE INDEX IF NOT EXISTS idx_resource_server_state_exchange_username
    ON dc_bot.resource_server_state (lower(exchange_username));
CREATE INDEX IF NOT EXISTS idx_resource_server_state_exchange_user_id
    ON dc_bot.resource_server_state (exchange_user_id);

--changeset dungeoncrusherbot:create_resource_server_history_table
CREATE TABLE IF NOT EXISTS dc_bot.resource_server_history
(
    id               BIGSERIAL PRIMARY KEY,
    server_state_id  BIGINT      NOT NULL REFERENCES dc_bot.resource_server_state (id) ON DELETE CASCADE,
    event_date       DATE        NOT NULL,
    resource         TEXT        NOT NULL,
    direction        TEXT        NOT NULL,
    quantity         INT         NOT NULL,
    from_server      INT
);

CREATE INDEX IF NOT EXISTS idx_resource_server_history_state_id
    ON dc_bot.resource_server_history (server_state_id);


--changeset dungeoncrusherbot:migrate_resource_server_state
INSERT INTO dc_bot.resource_server_state (
    user_id,
    server_id,
    exchange_label,
    exchange_username,
    exchange_user_id,
    draador_count,
    void_count,
    cb_count,
    balance,
    notify_disable,
    updated_at
)
SELECT
    r.user_id,
    (server.key)::INT                                          AS server_id,
    NULLIF(BTRIM(server.value->> 'exchange'), '')              AS exchange_label,
    NULL                                                       AS exchange_username,
    NULL                                                       AS exchange_user_id,
    COALESCE((server.value->> 'draadorCount')::INT, 0)         AS draador_count,
    COALESCE((server.value->> 'voidCount')::INT, 0)            AS void_count,
    COALESCE((server.value->> 'cbCount')::INT, 0)              AS cb_count,
    COALESCE((server.value->> 'balance')::INT, 0)              AS balance,
    COALESCE((server.value->> 'notifyDisable')::BOOLEAN, FALSE) AS notify_disable,
    now()                                                      AS updated_at
FROM dc_bot.resources r
    CROSS JOIN LATERAL jsonb_each(COALESCE(r.data-> 'servers', '{}'::jsonb)) AS server(key, value)
ON CONFLICT (user_id, server_id) DO NOTHING;

--changeset dungeoncrusherbot:migrate_resource_server_history
INSERT INTO dc_bot.resource_server_history (
    server_state_id,
    event_date,
    resource,
    direction,
    quantity,
    from_server
)
SELECT
    rss.id,
    COALESCE((item.value->> 'date')::DATE, CURRENT_DATE)      AS event_date,
    item.value->> 'resource'                                  AS resource,
    item.value->> 'type'                                      AS direction,
    COALESCE((item.value->> 'quantity')::INT, 0)              AS quantity,
    NULLIF(item.value->> 'fromServer', '')::INT               AS from_server
FROM dc_bot.resources r
    CROSS JOIN LATERAL jsonb_each(COALESCE(r.history, '{}'::jsonb)) AS history(key, value)
    CROSS JOIN LATERAL jsonb_array_elements(history.value) AS item(value)
    JOIN dc_bot.resource_server_state rss
        ON rss.user_id = r.user_id AND rss.server_id = (history.key)::INT;
