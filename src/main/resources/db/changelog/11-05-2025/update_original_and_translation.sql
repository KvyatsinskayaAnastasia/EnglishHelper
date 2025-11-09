--liquibase formatted sql

--changeset KvyatinskayaAnastasia:create_word
ALTER TABLE word ALTER COLUMN original TYPE varchar(150);
ALTER TABLE word ALTER COLUMN translation TYPE varchar(150);
--rollback ALTER TABLE word ALTER COLUMN original TYPE varchar(50);
--rollback ALTER TABLE word ALTER COLUMN translation TYPE varchar(50);