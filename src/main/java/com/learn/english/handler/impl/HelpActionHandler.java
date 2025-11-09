package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static com.learn.english.model.BotAction.HELP;

@Component
public class HelpActionHandler implements BotActionHandler {
    @Override
    public BotAction getBotAction() {
        return BotAction.HELP;
    }

    @Override
    public SendMessage processAction(UserState userState, String message) {
        return sendWithActionButtons(userState.getUserId(), HELP.getMessage(), BASE_OPTIONS, 2);
    }
}
