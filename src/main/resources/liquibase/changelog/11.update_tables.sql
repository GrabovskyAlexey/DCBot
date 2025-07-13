--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_1
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.1.1',
        e'Небольшие исправления текстов
Добавлена возможность учета долга по пустотам
Отображения баланса по картам на экране со всеми серверами
',
        false);

alter table users
    add column if not exists is_blocked boolean default false;
