--liquibase formatted sql

--changeset dungeoncrusherbot:add_deleted_flag_to_resource_history
ALTER TABLE IF EXISTS dc_bot.resource_server_history
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
