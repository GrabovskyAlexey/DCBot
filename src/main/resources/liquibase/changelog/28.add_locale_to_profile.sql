--liquibase formatted sql

--changeset dungeoncrusherbot:add locale to profile
ALTER TABLE IF EXISTS dc_bot.user_profile
    ADD COLUMN IF NOT EXISTS locale VARCHAR(2);