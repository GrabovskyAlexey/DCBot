--liquibase formatted sql

--changeset dc-bot:update_user_state_add_update_messages
alter table user_state
    add update_messages_by_state jsonb default '{}';

--changeset dc-bot:release_notes_1_0_6
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.0.6',
        e'Исправлен баг с уведомлениями, когда при выборе уведомления за 5 минут, так же приходило уведомление в момент осады
Исправлен баг когда при нажатии на кнопку обновлялось не привязанное к кнопке сообщение, а последнее
',
        false)