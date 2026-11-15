--liquibase formatted sql

--changeset KvyatinskayaAnastasia:make_repeat_at_nullable
ALTER TABLE word ALTER COLUMN repeat_at DROP NOT NULL;
--rollback ALTER TABLE word ALTER COLUMN repeat_at SET NOT NULL;
