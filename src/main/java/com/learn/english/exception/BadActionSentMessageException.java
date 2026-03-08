package com.learn.english.exception;

public class BadActionSentMessageException extends RuntimeException {
    public BadActionSentMessageException(String message) {
        super(message);
    }
}
