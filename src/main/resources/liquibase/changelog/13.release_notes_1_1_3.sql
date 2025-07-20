--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_3
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.1.3',
        e'Добавлена возможность создавать заметки(до 20шт)
Заметки можно создать через команду /notes
Они будут отображаться в меню сервера выбранного как основной
Переименована команда /notify в команду /settings
В /settings теперь можно включить учет КБ (по умолчанию отключен)
',
        false);

alter table users
    add if not exists settings jsonb default '{}';

alter table users
    add if not exists notes jsonb default '[]';

--changeset dc-bot:add_prev_state
alter table user_state
    add if not exists prev_state varchar;

