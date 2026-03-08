package com.learn.english.model;

import lombok.Getter;

@Getter
public enum BotAction {
    HELP("/help", "Помошь", "Выберите действие:"),
    ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL("/add_word_without_bot_request_original", "Добавить слово", "Добавление нового слова.\nВведите слово:"),
    ADD_WORD_WITHOUT_BOT_REQUEST_TRANSLATION("/add_word_without_bot_request_translation", null, "Добавление нового слова.\nВведите перевод слова %s"),
    ADD_WORD_WITHOUT_BOT_REQUEST_EXAMPLE("/add_word_without_bot_request_example", null, "Добавление нового слова.\nВведите пример с использованием слова %s в значении %s"),
    ADD_WORD_WITH_BOT_REQUEST_ORIGINAL("/add_word_with_bot_request_original", "Добавить слово (бот)", "Добавление нового слова с генерацией вариантов.\nВведите слово:"),
    ADD_WORD_WITH_BOT_REQUEST_TRANSLATION("/add_word_with_bot_request_translation", null, "Добавление нового слова с генерацией вариантов.\nВведите перевод слова %s или выберите из предложенных ботом"),
    ADD_WORD_WITH_BOT_REQUEST_EXAMPLE("/add_word_with_bot_request_example", null, "Добавление нового слова с генерацией вариантов..\nВведите пример с использованием слова %s в значении %s или выберите из предложенных ботом"),
    ADD_WORD_SAVE_WORD("/add_word_save_word", null, "Добавлено слово %s, в значении %s.\nПример использования: %s.\nВыберите действие:"),
    REPEATING_PLANNED("/repeating_planned", "Повторить по плану", null),
    REPEATING_RANDOM("/repeating_random", "Что-нибудь да повторить", null),
    REPEATING_CHOSE_TRANSLATION("/repeating_chose_translation", "Повторение: выбрать перевод", null),
    REPEATING_CHOOSE_ORIGINAL("/repeating_choose_original", "Повторение: выбрать слово по переводу", null),
    REPEATING_WRITE_WORD_BY_TRANSLATION("/repeating_write_word_by_translation", "Повторение: написать слово по переводу", null),
    REGENERATE_TRANSLATIONS("/regenerate_translations", "Перегенерировать переводы", "Переводы перегенерированы!\nДобавление нового слова.\nВведите перевод слова %s или выберите из предложенных ботом"),
    REGENERATE_EXAMPLES("/regenerate_examples", "Перегенерировать примеры", "Примеры перегенерированы!\nДобавление нового слова.\nВведите пример с использованием слова %s в значении %s или выберите из предложенных ботом"),
    SCHEDULED_REPEAT("/scheduled_repeat", null, null),
    EXIT("/exit", "Выйти", "Текущее действие завершено!\nВыберите действие:");

    private final String code;
    private final String buttonMessage;
    private final String answerMessage;

    BotAction(String code, String buttonMessage, String answerMessage) {
        this.code = code;
        this.buttonMessage = buttonMessage;
        this.answerMessage = answerMessage;
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
