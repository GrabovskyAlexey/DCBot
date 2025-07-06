--liquibase formatted sql

--changeset dc-bot:release_notes_1_0_6_1
INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.0.6.1',
        e'Изменено время уведомления о КШ на 2:59 и 14:59
',
        false)