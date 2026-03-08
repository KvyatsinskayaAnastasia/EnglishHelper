package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduledRepeatActionHandler implements BotActionHandler {
    private final WordService wordService;

    @Override
    public BotAction getBotAction() {
        return BotAction.SCHEDULED_REPEAT;
    }

    @Override
    public List<SendMessage> processAction(UserState userState, String message) {
        return List.of(SendMessage.builder()
                .chatId(userState.getUserId())
                .text(String.format("У вас %s неизученных слов. Вперед повторять!",
                        wordService.countWordsForRepeat(userState.getUserId())))
                .build());
    }
}
