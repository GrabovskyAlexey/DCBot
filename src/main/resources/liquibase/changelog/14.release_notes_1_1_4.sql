--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_4
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.1.4',
        e'Добавлена возможность добавлять или удалять заметки в меню основного сервера
В лабиринте добавлена возможность делать несколько шагов за 1 раз (не больше 10)
Добавлена возможность отправить отчет об ошибке или предложение в команде /settings
',
        false);

alter table maze
    add  if not exists same_steps boolean default false not null;

alter table users
    add if not exists is_admin boolean default false not null;