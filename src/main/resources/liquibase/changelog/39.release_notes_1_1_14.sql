--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_13
INSERT INTO dc_bot.update_messages (version, text, text_en, sent)
VALUES ('v.1.1.14',
        E'В поиске обменников добавлена возможность глобального поиска по всем вашим заявкам на всех серверах.
Теперь нет необходимости заходить на конкретный сервер и запускать поиск - это можно сделать прямо из главного меню с помощью команды /exchange.

Также в главном сообщении меню теперь отображаются все ваши заявки со всех серверов.
',
        E'A global search across all your exchange requests on all servers has been added to the exchange search.
You no longer need to enter a specific server and start the search there - everything can now be done directly from the main menu using the /exchange command.

In addition, the main menu message now displays all of your requests from all servers.',
        false);
