--liquibase formatted sql

--changeset dc-bot:update_user_state
alter table user_state
    add update_message_id integer;
alter table user_state
    add delete_message_ids jsonb;
alter table users
    add created_at timestamp default current_timestamp;
alter table users
    add updated_at timestamp default current_timestamp;