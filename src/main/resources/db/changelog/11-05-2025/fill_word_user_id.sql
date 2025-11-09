--liquibase formatted sql

--changeset KvyatinskayaAnastasia:create_word
UPDATE word
SET user_id = chat_id;