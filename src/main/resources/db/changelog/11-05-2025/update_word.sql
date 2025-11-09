--liquibase formatted sql

--changeset KvyatinskayaAnastasia:create_word
ALTER TABLE word ALTER COLUMN example_sentence TYPE varchar(300);
--rollback ALTER TABLE word ALTER COLUMN example_sentence TYPE varchar(100);