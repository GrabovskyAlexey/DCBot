--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_11
INSERT INTO dc_bot.update_messages (version, text, text_en, sent)
VALUES ('v.1.1.11',
        E'Добавлена возможность указать username обменника в Telegram.
Немного об особенности работы: указывая username вы соглашаетесь с тем что обменник сможет видеть количество карт у вас на руках, но при условии что этот сервер выбран у него основным.
Если вы хотите видеть запасы матрешек у обменника на своем сервере, который пометили как основной, попросите его указать ваш username у себя в боте.
Для того чтобы функционал работал, обменник тоже должен пользоваться ботом.
В ресурсах и лабиринте появилась отмена последнего действия.

Появилась инструкция по пользованию ботом. https://telegra.ph/Rukovodstvo-polzovatelya-Dungeon-Crusher-Bot-11-24',
        E'You can now specify an exchange partner\'s Telegram username.
A quick note on how it works: by adding a username you allow the partner to see how many maps you have on hand, but only if they set this server as their main one.
If you want to see an exchange partner\'s matryoshka stock on your main server, ask them to add your username in their bot.
The feature works only if the exchange partner also uses the bot.
Undo for the last action is now available in Resources and in the Maze.',
        false);
