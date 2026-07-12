package com.learn.english.service.impl;

import com.learn.english.user.handler.UserStatusHandler;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.TelegramService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordService;
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
    public void processUpdate(Update update) {
        Long userId = getUserId(update);
        UserState userState = userStateService.getUserState(userId);
        if (userState.getLastBotCommandMessageId() != null) {
            log.info("Clean bot command buttons");
            telegramHelperService.removeKeyboard(userState.getUserId(), userState.getLastBotCommandMessageId());
            userState.setLastBotCommandMessageId(null);
            userStateService.saveUserState(userState);
        }
        userStatusHandlers.get(userState.getUserStatus()).processAction(userState, update);
    }

    private Long getUserId(Update update) {
        return update.getCallbackQuery() != null
                ? update.getCallbackQuery().getFrom().getId()
                : update.getMessage().getFrom().getId();
    }
}