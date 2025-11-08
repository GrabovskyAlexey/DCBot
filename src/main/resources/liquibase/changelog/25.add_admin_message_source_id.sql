--liquibase formatted sql
--changeset dungeoncrusherbot:add-admin-message-source-id
ALTER TABLE dc_bot.admin_messages
    ADD COLUMN source_message_id INT;
