--liquibase formatted sql

--changeset KvyatinskayaAnastasia:drop_chat_id_from_word
DROP INDEX idx_word_repeat_at_chat_id;
DROP INDEX idx_word_chat_id;

CREATE INDEX idx_word_repeat_at_user_id ON word(repeat_at, user_id);
CREATE INDEX idx_word_user_id ON word(user_id);

ALTER TABLE word DROP COLUMN chat_id;
--rollback ALTER TABLE word ADD COLUMN chat_id bigint not null;