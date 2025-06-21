--liquibase formatted sql

--changeset dc-bot:create_schema
CREATE SCHEMA IF NOT EXISTS dc_bot;

--changeset dc-bot:create_tables
CREATE TABLE servers
(
    id   serial  NOT NULL,
    name varchar NOT NULL,
    CONSTRAINT servers_pkey PRIMARY KEY (id)
);

CREATE TABLE sieges
(
    id         serial NOT NULL,
    siege_time time   NOT NULL,
    CONSTRAINT sieges_pkey PRIMARY KEY (id)
);

CREATE TABLE users
(
    id         bigint NOT NULL,
    first_name varchar,
    last_name  varchar,
    username   varchar,
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

CREATE TABLE user_state
(
    user_id       bigint NOT NULL,
    state         varchar,
    callback_data varchar,
    CONSTRAINT user_state_pkey PRIMARY KEY (user_id)
);

CREATE TABLE user_subscribe
(
    user_id   bigint NOT NULL,
    server_id bigint NOT NULL,
    CONSTRAINT users_servers_pkey PRIMARY KEY (user_id, server_id),
    CONSTRAINT fk_user_subscribe_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_server_subscribe_id FOREIGN KEY (server_id) REFERENCES servers (id)
);

CREATE TABLE server_sieges
(
    server_id int NOT NULL,
    siege_id  int NOT NULL,
    CONSTRAINT servers_sieges_pkey PRIMARY KEY (server_id, siege_id),
    CONSTRAINT fk_server_sieges_siege_id FOREIGN KEY (siege_id) REFERENCES sieges (id),
    CONSTRAINT fk_server_sieges_server_id FOREIGN KEY (server_id) REFERENCES servers (id)
);

INSERT INTO sieges (id, siege_time) VALUES
                                        (1, '01:00:00'),
                                        (2, '02:00:00'),
                                        (3, '03:00:00'),
                                        (4, '04:00:00'),
                                        (5, '05:00:00'),
                                        (6, '06:00:00'),
                                        (7, '07:00:00'),
                                        (8, '08:00:00'),
                                        (9, '09:00:00'),
                                        (10, '10:00:00'),
                                        (11, '11:00:00'),
                                        (12, '12:00:00'),
                                        (13, '13:00:00'),
                                        (14, '14:00:00'),
                                        (15, '15:00:00'),
                                        (16, '16:00:00'),
                                        (17, '17:00:00'),
                                        (18, '18:00:00'),
                                        (19, '19:00:00'),
                                        (20, '20:00:00'),
                                        (21, '21:00:00'),
                                        (22, '22:00:00'),
                                        (23, '23:00:00'),
                                        (24, '00:00:00');

INSERT INTO servers (id, name) VALUES (1, '1 сервер'),
                                      (2, '2 сервер'),
                                      (3, '3 сервер'),
                                      (4, '4 сервер'),
                                      (5, '5 сервер'),
                                      (6, '6 сервер'),
                                      (7, '7 сервер'),
                                      (8, '8 сервер'),
                                      (9, '9 сервер'),
                                      (10, '10 сервер'),
                                      (11, '11 сервер'),
                                      (12, '12 сервер'),
                                      (13, '13 сервер'),
                                      (14, '14 сервер'),
                                      (15, '15 сервер'),
                                      (16, '16 сервер'),
                                      (17, '17 сервер');

INSERT INTO server_sieges (server_id, siege_id) VALUES (1, 3),
                                                       (1, 8),
                                                       (1, 13),
                                                       (1, 18),
                                                       (1, 23),
                                                       (5, 3),
                                                       (5, 8),
                                                       (5, 13),
                                                       (5, 18),
                                                       (5, 23),
                                                       (12, 3),
                                                       (12, 8),
                                                       (12, 13),
                                                       (12, 18),
                                                       (12, 23),
                                                       (16, 3),
                                                       (16, 8),
                                                       (16, 13),
                                                       (16, 18),
                                                       (16, 23),
                                                       (2, 4),
                                                       (2, 9),
                                                       (2, 14),
                                                       (2, 19),
                                                       (2, 24),
                                                       (6, 4),
                                                       (6, 9),
                                                       (6, 14),
                                                       (6, 19),
                                                       (6, 24),
                                                       (11, 4),
                                                       (11, 9),
                                                       (11, 14),
                                                       (11, 19),
                                                       (11, 24),
                                                       (13, 4),
                                                       (13, 9),
                                                       (13, 14),
                                                       (13, 19),
                                                       (13, 24),
                                                       (17, 4),
                                                       (17, 9),
                                                       (17, 14),
                                                       (17, 19),
                                                       (17, 24),
                                                       (3, 1),
                                                       (3, 5),
                                                       (3, 10),
                                                       (3, 15),
                                                       (3, 20),
                                                       (7, 1),
                                                       (7, 5),
                                                       (7, 10),
                                                       (7, 15),
                                                       (7, 20),
                                                       (10, 1),
                                                       (10, 5),
                                                       (10, 10),
                                                       (10, 15),
                                                       (10, 20),
                                                       (14, 1),
                                                       (14, 5),
                                                       (14, 10),
                                                       (14, 15),
                                                       (14, 20),
                                                       (4, 2),
                                                       (4, 6),
                                                       (4, 11),
                                                       (4, 16),
                                                       (4, 21),
                                                       (8, 2),
                                                       (8, 6),
                                                       (8, 11),
                                                       (8, 16),
                                                       (8, 21),
                                                       (9, 2),
                                                       (9, 6),
                                                       (9, 11),
                                                       (9, 16),
                                                       (9, 21),
                                                       (15, 2),
                                                       (15, 6),
                                                       (15, 11),
                                                       (15, 16),
                                                       (15, 21);
