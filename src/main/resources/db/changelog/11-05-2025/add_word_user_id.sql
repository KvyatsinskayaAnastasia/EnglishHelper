--liquibase formatted sql

--changeset KvyatinskayaAnastasia:create_word
ALTER TABLE word
    ADD COLUMN user_id bigint not null default 0;
ALTER TABLE word
    ALTER COLUMN user_id DROP DEFAULT;
--rollback ALTER TABLE word DROP COLUMN user_id;