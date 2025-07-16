--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_2
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.1.2',
        e'Добавлена возможность выбрать основной сервер при учете ресурсов
Добавлена возможность отключить уведомления по определенным серверам до конца недели
Уведомления отключатся в меню учета ресурсов сервера
',
        false);

