--liquibase formatted sql

--changeset dc-bot:create_user_profile_table
CREATE TABLE IF NOT EXISTS dc_bot.user_profile
(
    user_id        BIGINT PRIMARY KEY REFERENCES dc_bot.users (id),
    is_blocked     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_admin       BOOLEAN      NOT NULL DEFAULT FALSE,
    settings       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    notes          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    main_server_id INTEGER
);

--changeset dc-bot:migrate_user_profile_values
INSERT INTO dc_bot.user_profile (user_id, is_blocked, is_admin, settings, notes, main_server_id)
SELECT
    u.id,
    COALESCE(u.is_blocked, FALSE),
    COALESCE(u.is_admin, FALSE),
    COALESCE(u.settings, '{}'::jsonb),
    COALESCE(u.notes, '[]'::jsonb),
    CASE
        WHEN jsonb_exists(COALESCE(r.data, '{}'::jsonb), 'mainServerId')
            THEN NULLIF(COALESCE(r.data, '{}'::jsonb) ->> 'mainServerId', '')::INT
        ELSE NULL
    END
FROM dc_bot.users u
LEFT JOIN dc_bot.resources r ON r.user_id = u.id
ON CONFLICT (user_id) DO NOTHING;

--changeset dc-bot:drop_user_columns
ALTER TABLE dc_bot.users
    DROP COLUMN IF EXISTS is_blocked,
    DROP COLUMN IF EXISTS is_admin,
    DROP COLUMN IF EXISTS settings,
    DROP COLUMN IF EXISTS notes;

--changeset dc-bot:remove_main_server_from_resources_json
UPDATE dc_bot.resources
SET data = data - 'mainServerId'
WHERE jsonb_exists(COALESCE(data, '{}'::jsonb), 'mainServerId');
