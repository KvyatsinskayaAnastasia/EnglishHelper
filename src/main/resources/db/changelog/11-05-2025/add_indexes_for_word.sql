--liquibase formatted sql

--changeset KvyatinskayaAnastasia:add_indexes_for_word
CREATE INDEX idx_word_repeat_at_chat_id ON word(repeat_at, chat_id);
CREATE INDEX idx_word_distinct_users ON word(repeat_at);
CREATE INDEX idx_word_chat_id ON word(chat_id);