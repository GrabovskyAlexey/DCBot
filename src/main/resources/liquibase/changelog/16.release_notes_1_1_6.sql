--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_6
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.1.6',
        e'Небольшие косметические улучшения',
        false);

CREATE TABLE IF NOT EXISTS admin_messages
(
    id bigserial  NOT NULL,
    user_id bigint NOT NULL,
    message text NOT NULL,
    CONSTRAINT admin_messages_pkey PRIMARY KEY (id)
);
