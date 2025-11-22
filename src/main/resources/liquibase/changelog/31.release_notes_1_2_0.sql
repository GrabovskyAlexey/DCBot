--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_10
INSERT INTO dc_bot.update_messages (version, text, text_en, sent)
VALUES ('v.1.1.10',
        E'Добавлен учёт долгов: фиксируйте кому должны и кто должен вам для пустот, карт, КБ и ящиков банок. Команда /debt

*Так же создана группа в Telegram по боту: https://t.me/+0u7LhHqEny84ZGIy*',
        E'Debt tracking has been added: keep records of who owes you and whom you owe for Voids, Maps, DH, and box flasks. Use the /debt command.

*A Telegram group for the bot has also been created: https://t.me/+0u7LhHqEny84ZGIy*',
        false);
