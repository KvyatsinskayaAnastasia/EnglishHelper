--liquibase formatted sql

--changeset KvyatinskayaAnastasia:create_word
create table word
(
    id               uuid        not null primary key,
    original         varchar(50) not null,
    translation      varchar(50) not null,
    example_sentence varchar(100),
    repeat_at        timestamp   not null,
    repeated_count   integer     not null,
    chat_id          bigint      not null,
    created_at       timestamp   not null,
    constraint original_translation_user_id_uniq unique (original, translation, chat_id)
);

--rollback drop table wordState;