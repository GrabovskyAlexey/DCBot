--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_9
INSERT INTO dc_bot.update_messages (version, text, text_en, sent)
VALUES ('v.1.1.9',
        E'Добавлена возможность отключить добавление карт на основной сервер, когда нажимается кнопка получить на других серверах
Добавлена возможность переключать язык интерфейса между русским и английским, теперь язык не привязан к настройкам Telegram
Переработана внутренняя логика бота

*‼️Внимание ‼️
Из-за переработки логики кнопки в старых сообщениях с ресурсами, настройками лабиринтом и т.д. могут не работать, достаточно просто заново вызвать эти сообщения через меню*',
        E'Added the option to disable adding maps to the main server when pressing the “Receive” button on other servers.
Added the ability to switch the interface language between Russian and English — it’s no longer tied to your Telegram language settings.
The bot’s internal logic has been reworked.

*‼️Attention‼️
Due to the logic update, buttons in older messages (such as those with resources, settings, or maze) may no longer work. Simply reopen these messages through the menu.*',
        false);
