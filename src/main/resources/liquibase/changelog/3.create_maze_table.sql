--liquibase formatted sql

--changeset dc-bot:create_maze_table
CREATE TABLE maze
(
    id   serial  NOT NULL,
    user_id bigint NOT NULL,
    current_location jsonb,
    steps jsonb,
    CONSTRAINT maze_pkey PRIMARY KEY (id)
);