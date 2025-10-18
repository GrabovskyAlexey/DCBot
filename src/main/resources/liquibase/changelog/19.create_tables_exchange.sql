--liquibase formatted sql
--changeset dc-bot:create_exchange_tables
CREATE TABLE exchange_requests
(
    id                    bigserial PRIMARY KEY,
    type                  text        NOT NULL, -- EXCHANGE_MAP, EXCHANGE_VOID, SELL_MAP, BUY_MAP
    user_id               BIGINT      NOT NULL REFERENCES users (id),
    source_server_id      int         NOT NULL REFERENCES servers (id),
    target_server_id      int REFERENCES servers (id),
    -- что отдаю
    source_resource_type  text        NOT NULL, -- MAP, VOID
    target_resource_type  text        NOT NULL, -- MAP, VOID
    source_resource_price int         NOT NULL,
    target_resource_price int         NOT NULL,
    is_active             boolean     not null default false,
    -- служебное
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

--changeset dc-bot:add-column-server-id-by-state
ALTER TABLE user_state
    ADD COLUMN IF NOT EXISTS last_server_id_by_state jsonb default '{}';


--changeset dc-bot:add-callback-data-table
CREATE TABLE IF NOT EXISTS callback_data
(
    id      bigserial PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    type    text        NOT NULL,
    data    text  not null
)