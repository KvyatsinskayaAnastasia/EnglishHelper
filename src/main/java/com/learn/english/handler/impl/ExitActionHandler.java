package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class ExitActionHandler implements BotActionHandler {
    @Override
    public BotAction getBotAction() {
        return BotAction.EXIT;
    }

    @Override
    public SendMessage processAction(UserState userState, String message) {
        userState.setCurrentWordState(null);
        userState.setProposesState(null);
        userState.setRepeatingState(null);
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        return sendWithActionButtons(userState.getUserId(), "Выберите действие", BASE_OPTIONS, 2);
    }
}
