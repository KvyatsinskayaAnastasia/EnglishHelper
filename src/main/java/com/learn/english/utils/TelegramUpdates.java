package com.learn.english.utils;

import org.telegram.telegrambots.meta.api.objects.Update;

public final class TelegramUpdates {
    private TelegramUpdates() {
    }

    public static Long userId(Update update) {
        if (update == null) {
            return null;
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            return update.getCallbackQuery().getFrom().getId();
        }
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            return update.getMessage().getFrom().getId();
        }
        return null;
    }
}
