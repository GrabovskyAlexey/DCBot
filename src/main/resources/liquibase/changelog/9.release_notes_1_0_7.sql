--liquibase formatted sql

--changeset dc-bot:release_notes_1_0_7
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.0.7',
        e'Исправелна ошибка с невозможностью сбросить прогресс лабиринта
',
        false)