package com.learn.english.model;

import lombok.Getter;

@Getter
public enum PromptType {
    TRANSLATE_WORD_TO_RUSSIAN("TRANSLATE_WORD_TO_RUSSIAN",
            "Предложи переводы слова %s на русский язык. В ответ выведи json-массив с возможными переводами в формате: [\"перевод1\", \"перевод2\", \"перевод3\"]"),
    PROPOSE_EXAMPLES_OF_USING("PROPOSE_EXAMPLES_OF_USING",
            "Предложи 3 примера использования слова \"%s\" (%s) на английском языке. В ответ выведи json-массив с возможными примерами в формате: [\"example1\", \"example2\", \"example3\"]");

    private String code;
    private String prompt;

    PromptType(String code, String prompt) {
        this.code = code;
        this.prompt = prompt;
    }
}
