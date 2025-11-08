--liquibase formatted sql

--changeset dungeoncrusherbot:cleanup_unused_resources
ALTER TABLE IF EXISTS dc_bot.resources
    DROP COLUMN IF EXISTS last_server_id;

ALTER TABLE IF EXISTS resources
    DROP COLUMN IF EXISTS last_server_id;

DROP TABLE IF EXISTS dc_bot.callback_data CASCADE;

DROP TABLE IF EXISTS callback_data CASCADE;
