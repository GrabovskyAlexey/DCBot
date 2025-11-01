--liquibase formatted sql

--changeset dc-bot:drop_user_state_table
DROP TABLE IF EXISTS dc_bot.user_state CASCADE;
DROP TABLE IF EXISTS user_state CASCADE;
