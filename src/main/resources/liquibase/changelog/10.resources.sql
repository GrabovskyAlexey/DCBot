--liquibase formatted sql

--changeset dc-bot:release_notes_1_1_0
CREATE TABLE IF NOT EXISTS resources
(
    id   serial  NOT NULL,
    user_id bigint NOT NULL,
    data jsonb,
    history jsonb,
    last_server_id int,
    CONSTRAINT resources_pkey PRIMARY KEY (id)
);

INSERT INTO update_messages (version, text, sent)
VALUES ('v.1.1.0',
        e'Добавлена возможность учета матрешек и пустот на серверах
',
        false);

--changeset poi-bot:create_table_verification_request
CREATE TABLE IF NOT EXISTS verification_request
(
    id bigserial PRIMARY KEY NOT NULL,
    message text,
    state VARCHAR(255),
    result boolean
);

alter table user_state
    add column if not exists verification_request_id bigint;
