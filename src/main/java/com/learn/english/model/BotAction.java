package com.learn.english.model;

import lombok.Getter;

@Getter
public enum BotAction {
    HELP("/help", "Выберите действие"),
    ADD_WORD_REQUEST_ORIGINAL("/add_word_request_original", "Добавить слово"),
    ADD_WORD_REQUEST_TRANSLATION("/add_word_request_translation", "Введите перевод"),
    ADD_WORD_REQUEST_EXAMPLE("/add_word_request_example", "Введите пример с использованием слова"),
    ADD_WORD_WITH_BOT_REQUEST_ORIGINAL("/add_word_with_bot_request_original", "Добавить слово (бот)"),
    ADD_WORD_WITH_BOT_REQUEST_TRANSLATION("/add_word_with_bot_request_translation", "Введите перевод или выберите из предложенных ботом"),
    ADD_WORD_WITH_BOT_REQUEST_EXAMPLE("/add_word_with_bot_request_example", "Введите пример с использованием слова или выберите из предложенных ботом"),
    ADD_WORD_SAVE_WORD("/add_word_save_word", "Слово сохранено! Выберите опцию"),
    REPEATING_PLANNED("/repeating_planned", "Повторить по плану"),
    REPEATING_RANDOM("/repeating_random", "Что-нибудь да повторить"),
    REPEATING_CHOSE_TRANSLATION("/repeating_chose_translation", "Повторение: выбрать перевод"),
    REPEATING_CHOOSE_ORIGINAL("/repeating_choose_original", "Повторение: выбрать слово по переводу"),
    REPEATING_WRITE_WORD_BY_TRANSLATION("/repeating_write_word_by_translation", "Повторение: написать слово по переводу"),
    REGENERATE_TRANSLATIONS("/regenerate_translations", "Перегенерировать переводы"),
    REGENERATE_EXAMPLES("/regenerate_examples", "Перегенерировать примеры"),
    SCHEDULED_REPEAT("/scheduled_repeat", ""),
    EXIT("/exit", "Выйти");

    private final String code;
    private final String message;

    BotAction(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static BotAction getOptionFromValue(String code) {
        for (BotAction opt : BotAction.values()) {
            if (opt.code.equals(code)) {
                return opt;
            }
        }
       return null;
    }
}
