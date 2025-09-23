--liquibase formatted sql

--changeset dc-bot:update_users_language_column
ALTER TABLE dc_bot.users
    ADD COLUMN IF NOT EXISTS language varchar(16);

--changeset dc-bot:update_users_last_action_column
ALTER TABLE dc_bot.users
    ADD COLUMN IF NOT EXISTS last_action_at TIMESTAMP WITH TIME ZONE;

--changeset dc-bot:populate_last_action_at
UPDATE dc_bot.users
SET last_action_at = COALESCE(last_action_at, NOW());

--changeset dc-bot:update_messages_add_text_en
ALTER TABLE dc_bot.update_messages
    ADD COLUMN IF NOT EXISTS text_en TEXT;

--changeset dc-bot:update_release_notes_1_1_7
UPDATE dc_bot.update_messages
SET text = E'- Добавлены быстрые кнопки +1/-1 для всех ресурсов.\n- Уведомления по шахтам теперь показывают правильное время даже после переключения вкладок.\n- Релиз-ноты отправляются на языке Telegram-профиля: русский по умолчанию, английский для остальных.',
    text_en = E'- Added quick +1/-1 buttons for every resource.\n- Mine notifications now display the correct time even after switching tabs.\n- Release notes now follow your Telegram language: Russian by default, English otherwise.'
WHERE version = 'v.1.1.7';
