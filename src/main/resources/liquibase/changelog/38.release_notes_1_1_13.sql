--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_13
INSERT INTO dc_bot.update_messages (version, text, text_en, sent)
VALUES ('v.1.1.13',
        E'В настройках появилась возможность выбрать, что отображать в главной сводке ресурсов: имя обменника или его @username в Telegram.

Настройка находится в разделе настроек (⚙️ /settings).
По умолчанию отображается имя обменника (текущее поведение сохранено).
При выборе отображения @username:
  • Показывается @username там, где он указан
  • Если @username не указан, показывается имя обменника

Настройка влияет только на главную сводку ресурсов, детальные сообщения по серверам не изменяются.',
        E'You can now choose what to display in the main resources summary: exchange partner name or their Telegram @username.

The setting is available in the settings section (⚙️ /settings).
By default, the exchange partner name is shown (current behavior preserved).
When @username display is enabled:
  • Shows @username where it is specified
  • Falls back to partner name if @username is not set

This setting only affects the main resources summary, detailed server messages remain unchanged.',
        false);
