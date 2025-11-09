--liquibase formatted sql

--changeset KvyatinskayaAnastasia:create_words_archive
create table words_archive
(
    id               uuid         not null primary key,
    original         varchar(150) not null,
    translation      varchar(150) not null,
    example_sentence varchar(300),
    user_id          bigint       not null,
    created_at       timestamp    not null
);

--rollback drop table words_archive;