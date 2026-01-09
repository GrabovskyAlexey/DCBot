--liquibase formatted sql

--changeset dungeoncrusherbot:add_resource_history_prev_values
ALTER TABLE IF EXISTS dc_bot.resource_server_history
    ADD COLUMN IF NOT EXISTS prev_draador_count INT,
    ADD COLUMN IF NOT EXISTS prev_void_count INT,
    ADD COLUMN IF NOT EXISTS prev_cb_count INT,
    ADD COLUMN IF NOT EXISTS prev_balance INT;
