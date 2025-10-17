--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_8
INSERT INTO dc_bot.update_messages (version, text, text_en, sent)
VALUES ('v.1.1.8',
        E'Добавлен функционал поиска обменников или покупателей. Воспользоваться им можно через меню или команду /exchange.
Важный нюанс: пока у бота немного пользователей, велика вероятность, что обменник не найдётся.
Не стоит пока возлагать на эту функцию больших надежд — с ростом аудитории бота результаты поиска будут точнее.

💡 Если хочешь помочь ускорить развитие функции — расскажи о боте друзьям или гильдии! Чем больше пользователей, тем проще всем находить обмены.',
        E'Added a new feature for finding exchange partners or buyers. You can use it via the menu or the /exchange command.
One important note: since the bot doesn’t have many users yet, there’s a good chance no match will be found.
Don’t expect too much from this feature just yet — as the community grows, search results will become more accurate.

💡 If you’d like to help the feature improve faster — share the bot with your friends or guild! The more users join, the easier it becomes for everyone to find exchanges.',
        false);
