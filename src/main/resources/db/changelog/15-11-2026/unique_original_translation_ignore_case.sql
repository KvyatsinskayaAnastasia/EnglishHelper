--liquibase formatted sql

--changeset KvyatinskayaAnastasia:unique_original_translation_ignore_case
UPDATE word
SET original = btrim(original),
    translation = btrim(translation),
    example_sentence = NULLIF(btrim(example_sentence), '')
WHERE original IS DISTINCT FROM btrim(original)
   OR translation IS DISTINCT FROM btrim(translation)
   OR example_sentence IS DISTINCT FROM NULLIF(btrim(example_sentence), '');

DELETE FROM word
WHERE id IN (
    SELECT id
    FROM (
             SELECT id,
                    ROW_NUMBER() OVER (
                        PARTITION BY user_id, lower(original), lower(translation)
                        ORDER BY repeated_count DESC, created_at ASC, id ASC
                        ) AS row_number
             FROM word
         ) ranked
    WHERE ranked.row_number > 1
);

ALTER TABLE word DROP CONSTRAINT IF EXISTS original_translation_user_id_uniq;

CREATE UNIQUE INDEX original_translation_user_id_uniq
    ON word (lower(original), lower(translation), user_id);

--rollback DROP INDEX IF EXISTS original_translation_user_id_uniq;
--rollback ALTER TABLE word ADD CONSTRAINT original_translation_user_id_uniq UNIQUE (original, translation, user_id);
