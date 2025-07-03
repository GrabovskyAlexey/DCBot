--liquibase formatted sql

--changeset dc-bot:release_notes_1_0_5
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.0.5',
        e'Исправлены мелкие баи с уведомлениями
Переработано сообщение настройки уведомления, оно сделано более информативным
',
        false)