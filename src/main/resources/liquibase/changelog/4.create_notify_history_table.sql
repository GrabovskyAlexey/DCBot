--liquibase formatted sql

--changeset dc-bot:create_notify_history_table
CREATE TABLE if not exists notify_history
(
    id   serial  NOT NULL,
    user_id bigint NOT NULL,
    message_id int NOT NULL,
    text text,
    send_time timestamp default (current_timestamp),
    deleted bool default false,
    CONSTRAINT notify_history_pkey PRIMARY KEY (id)
);

CREATE INDEX notify_history_idx_1 on notify_history (deleted);
CREATE INDEX notify_history_idx_2 on notify_history (user_id, deleted);
CREATE INDEX notify_history_idx_3 on notify_history (user_id, send_time);