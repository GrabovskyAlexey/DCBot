--liquibase formatted sql
--changeset dc-bot:create_exchange_tables
CREATE TABLE exchange_requests
(
    id bigserial PRIMARY KEY,
    type text NOT NULL, -- EXCHANGE_MAP, EXCHANGE_VOID, SELL_MAP, BUY_MAP
    user_id   BIGINT         NOT NULL REFERENCES users (id),
    source_server_id    int       NOT NULL REFERENCES servers (id),
    target_server_id    int REFERENCES servers (id),
    -- что отдаю
    source_resource_type text NOT NULL, -- MAP, VOID
    target_resource_type text NOT NULL, -- MAP, VOID
    source_resource_price int NOT NULL,
    target_resource_price int NOT NULL,
    is_active boolean not null default false,
    -- служебное
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);