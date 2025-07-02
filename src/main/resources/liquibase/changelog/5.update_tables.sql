--liquibase formatted sql

--changeset dc-bot:create_notify_setting_and_release_notes_tables
CREATE TABLE IF NOT EXISTS notification_subscribe
(
    id      bigserial NOT NULL,
    user_id bigint    NOT NULL,
    type    varchar,
    enabled boolean default true,
    CONSTRAINT notification_subscribe_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS update_messages
(
    id      bigserial NOT NULL,
    version varchar    NOT NULL,
    text    text,
    sent    boolean default false,
    CONSTRAINT update_messages_pkey PRIMARY KEY (id)
);

INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.0.4',
        e'Добавлена возможность включить уведомления о перехвате КШ
Уведомления будут присылаться в 03:00 и 15:00
Для включения или отключения уведомлений используйте команду /notify
Там же добавлена возможность включить уведомления об осадах за 5 минут до начала
Уменьшено время жизни уведомлений об осадах и КШ до 2 часов(было 12 часов)
',
        false)