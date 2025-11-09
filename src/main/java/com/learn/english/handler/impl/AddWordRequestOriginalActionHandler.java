package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static com.learn.english.model.BotAction.EXIT;

@Component
public class AddWordRequestOriginalActionHandler implements BotActionHandler {
    @Override
    public BotAction getBotAction() {
        return BotAction.ADD_WORD_REQUEST_ORIGINAL;
    }

    @Override
    public SendMessage processAction(UserState userState, String message) {
        userState.setCurrentWordState(new WordState());
        userState.setUserStatus(UserStatus.FILLING_WORD_WITHOUT_BOT);
        return sendWithActionButtons(userState.getUserId(), "Введите слово:", List.of(EXIT), 1);
    }
}
