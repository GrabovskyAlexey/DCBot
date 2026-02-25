--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_13
INSERT INTO dc_bot.update_messages (version, text, text_en, sent)
VALUES ('v.1.1.15',
        E'Исправление ошибок',
        E'Bugfixes',
        false);
