package com.learn.english.service.impl;

import com.learn.english.user.handler.UserStatusHandler;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.TelegramService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordService;
import com.learn.english.utils.TelegramUpdates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseTelegramService implements TelegramService {
    private final Map<UserStatus, UserStatusHandler> userStatusHandlers;
    private final TelegramHelperService telegramHelperService;
    private final UserStateService userStateService;
    private final WordService wordService;

    @Override
    public void processScheduledRepeat(long userId) {
        UserState userState = userStateService.getUserState(userId);
        telegramHelperService.sendMessage(SendMessage.builder()
                .chatId(userState.getUserId())
                .text(String.format("У вас %s неизученных слов. Вперед повторять!",
                        wordService.countWordsForRepeat(userState.getUserId())))
                .build());
    }

    @Override
    public void notifyProcessingFailed(long chatId) {
        telegramHelperService.sendMessage(SendMessage.builder()
                .chatId(chatId)
                .text("Не получилось обработать сообщение. Попробуйте ещё раз.")
                .build());
    }

    @Override
    public void processUpdate(Update update) {
        if (update.getCallbackQuery() != null) {
            telegramHelperService.answerCallbackQuery(update.getCallbackQuery().getId());
        }
        Long userId = getUserId(update);
        if (userId == null) {
            log.debug("Skipping update {} without a user", update.getUpdateId());
            return;
        }
        UserState userState = userStateService.getUserState(userId);
        if (userState.getLastBotCommandMessageId() != null) {
            clearPreviousKeyboard(userState);
        }
        UserStatusHandler handler = userStatusHandlers.get(userState.getUserStatus());
        if (handler == null) {
            log.warn("No handler for status {}, resetting user {}", userState.getUserStatus(), userId);
            resetToMenu(userState);
            handler = userStatusHandlers.get(UserStatus.NO_ACTIVITY);
        }
        if (handler == null) {
            log.error("NO_ACTIVITY handler is not configured");
            return;
        }
        handler.processAction(userState, update);
    }

    private void clearPreviousKeyboard(UserState userState) {
        Integer messageId = userState.getLastBotCommandMessageId();
        try {
            telegramHelperService.removeKeyboard(userState.getUserId(), messageId);
        } catch (RuntimeException e) {
            log.error("Failed to remove inline keyboard: userId={}, messageId={}",
                    userState.getUserId(), messageId, e);
        }
        userState.setLastBotCommandMessageId(null);
        userStateService.saveUserState(userState);
    }

    private static void resetToMenu(UserState userState) {
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        userState.setCurrentWordState(null);
        userState.setProposesState(null);
        userState.setRepeatingState(null);
    }

    private Long getUserId(Update update) {
        return TelegramUpdates.userId(update);
    }
}