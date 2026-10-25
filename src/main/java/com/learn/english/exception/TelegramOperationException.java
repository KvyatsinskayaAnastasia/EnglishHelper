package com.learn.english.exception;

public class TelegramOperationException extends RuntimeException {

    public TelegramOperationException(String operation, Object chatId, Integer messageId, Throwable cause) {
        super("Telegram API error during '%s' (chatId=%s, messageId=%s)"
                .formatted(operation, chatId, messageId), cause);
    }
}