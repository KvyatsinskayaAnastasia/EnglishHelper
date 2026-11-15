--liquibase formatted sql

--changeset KvyatinskayaAnastasia:add_original_translation_user_unique
DELETE FROM word
WHERE id IN (
    SELECT id
    FROM (
             SELECT id,
                    ROW_NUMBER() OVER (
                        PARTITION BY user_id, original, translation
                        ORDER BY repeated_count DESC, created_at ASC, id ASC
                        ) AS row_number
             FROM word
         ) ranked
    WHERE ranked.row_number > 1
);

ALTER TABLE word
    ADD CONSTRAINT original_translation_user_id_uniq UNIQUE (original, translation, user_id);

--rollback ALTER TABLE word DROP CONSTRAINT original_translation_user_id_uniq;
