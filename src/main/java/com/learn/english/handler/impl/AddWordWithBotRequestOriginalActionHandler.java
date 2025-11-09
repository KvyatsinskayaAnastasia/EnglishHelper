package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.ProposesState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class AddWordWithBotRequestOriginalActionHandler implements BotActionHandler {
    @Override
    public BotAction getBotAction() {
        return BotAction.ADD_WORD_WITH_BOT_REQUEST_ORIGINAL;
    }

    @Override
    public SendMessage processAction(UserState userState, String message) {
        userState.setCurrentWordState(new WordState());
        userState.setProposesState(new ProposesState());
        userState.setUserStatus(UserStatus.FILLING_WORD);
        return SendMessage
                .builder()
                .chatId(userState.getUserId())
                .text("Введите слово")
                .build();
    }
}
