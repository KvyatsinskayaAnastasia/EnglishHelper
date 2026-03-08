package com.learn.english.exception;

import com.learn.english.model.BotAction;
import com.learn.english.model.UserStatus;

public class BadUserStatusException extends RuntimeException {
    private static final String EXCEPTION_MESSAGE = "Action %s disabled from user status %s";

    public BadUserStatusException(BotAction action, UserStatus status) {
        super(String.format(EXCEPTION_MESSAGE, action, status));
    }
}
