--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_7
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.1.7',
        e'Добавлены быстрые кнопки +1/-1 для учёта ресурсов.
Включите режим в настройках и обновляйте драадоров, пустоты и КБ без ручного ввода.',
        false);
