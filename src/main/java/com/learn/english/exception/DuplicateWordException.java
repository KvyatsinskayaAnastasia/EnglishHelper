package com.learn.english.exception;

public class DuplicateWordException extends RuntimeException {

    public DuplicateWordException(String original, String translation) {
        super("Card already exists for original '%s' and translation '%s'".formatted(original, translation));
    }
}
