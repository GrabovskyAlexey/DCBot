--liquibase formatted sql

--changeset dungeoncrusherbot:drop_resources_table
DROP TABLE IF EXISTS dc_bot.resources CASCADE;
DROP TABLE IF EXISTS resources CASCADE;
