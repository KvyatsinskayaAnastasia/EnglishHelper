package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_REQUEST_EXAMPLE;
import static com.learn.english.model.BotAction.EXIT;

@Component
public class AddWordRequestExampleActionHandler implements BotActionHandler {
    @Override
    public BotAction getBotAction() {
        return BotAction.ADD_WORD_REQUEST_EXAMPLE;
    }

    @Override
    public SendMessage processAction(UserState userState, String message) {
        userState.getCurrentWordState().setTranslation(message);
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE_WITHOUT_BOT);
        return sendWithActionButtons(userState.getUserId(),
                ADD_WORD_REQUEST_EXAMPLE.getMessage(), List.of(EXIT), 1);
    }
}
