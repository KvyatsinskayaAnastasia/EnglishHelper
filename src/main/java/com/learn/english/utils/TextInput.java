package com.learn.english.utils;

import com.learn.english.model.UserState;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

import static com.learn.english.model.BotAction.EXIT;

public final class TextInput {
    public static final int WORD_LIMIT = 150;
    public static final int EXAMPLE_LIMIT = 300;
    public static final String MEDIA_REJECTED = "Стикеры и фото не принимаются. Пришлите текст.";
    public static final String TEXT_REQUIRED = "Пришлите текст.";

    private TextInput() {
    }

    public static boolean isStickerOrPhoto(Update update) {
        Message message = update == null ? null : update.getMessage();
        return message != null && (message.hasSticker() || message.hasPhoto());
    }

    public static Optional<String> rejection(Update update, int limit) {
        if (isStickerOrPhoto(update)) {
            return Optional.of(MEDIA_REJECTED);
        }
        String text = value(update);
        if (text == null) {
            return Optional.of(TEXT_REQUIRED);
        }
        if (exceeds(text, limit)) {
            return Optional.of(tooLong(limit));
        }
        return Optional.empty();
    }

    public static String value(Update update) {
        if (update == null || update.getMessage() == null || !update.getMessage().hasText()) {
            return null;
        }
        String text = update.getMessage().getText();
        if (text == null) {
            return null;
        }
        text = text.trim();
        if (text.isEmpty()) {
            return null;
        }
        return text;
    }

    public static boolean exceeds(String text, int limit) {
        return text.codePointCount(0, text.length()) > limit;
    }

    public static String tooLong(int limit) {
        return "Слишком длинный текст. Максимум " + limit + " символов.";
    }

    public static void keepAsking(UserState userState, TelegramHelperService telegramHelperService,
                                  UserStateService userStateService, String text) {
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                ButtonUtils.sendWithActionButtons(userState.getUserId(), text, List.of(EXIT), 1)));
        userStateService.saveUserState(userState);
    }

    public static Optional<String> accept(UserState userState, TelegramHelperService telegramHelperService,
                                           UserStateService userStateService, Update update, int limit) {
        Optional<String> rejection = rejection(update, limit);
        if (rejection.isPresent()) {
            keepAsking(userState, telegramHelperService, userStateService, rejection.get());
            return Optional.empty();
        }
        return Optional.of(value(update));
    }
}
