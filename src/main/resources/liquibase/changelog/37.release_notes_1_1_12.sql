--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_11
INSERT INTO dc_bot.update_messages (version, text, text_en, sent)
VALUES ('v.1.1.12',
        E'Исправление ошибок',
        E'Minor bugfixes',
        false);
