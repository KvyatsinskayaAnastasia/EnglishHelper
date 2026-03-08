package com.learn.english.model;

import com.vdurmont.emoji.EmojiParser;

public enum Icon {
    NOT(":x:"),
    CHECK(":white_check_mark:");

    private final String value;

    public String get() {
        return EmojiParser.parseToUnicode(value);
    }

    Icon(String value) {
        this.value = value;
    }
}