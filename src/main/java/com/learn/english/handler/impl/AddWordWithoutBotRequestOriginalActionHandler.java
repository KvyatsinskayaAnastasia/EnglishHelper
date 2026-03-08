package com.learn.english.handler.impl;

import com.learn.english.exception.BadUserStatusException;
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
public class AddWordWithoutBotRequestOriginalActionHandler implements BotActionHandler {
    @Override
    public BotAction getBotAction() {
        return BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL;
    }

    @Override
    public List<SendMessage> processAction(UserState userState, String message) {
        if (userState.getUserStatus() != UserStatus.NO_ACTIVITY) {
            throw new BadUserStatusException(getBotAction(), userState.getUserStatus());
        }
        userState.setCurrentWordState(new WordState());
        userState.setUserStatus(UserStatus.FILLING_ORIGINAL_WITHOUT_BOT);
        return List.of(sendWithActionButtons(userState.getUserId(), BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL.getAnswerMessage(),
                List.of(EXIT), 1));
    }
}
