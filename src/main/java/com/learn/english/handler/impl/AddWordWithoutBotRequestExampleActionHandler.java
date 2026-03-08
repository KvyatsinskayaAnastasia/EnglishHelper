package com.learn.english.handler.impl;

import com.learn.english.exception.BadActionSentMessageException;
import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_EXAMPLE;
import static com.learn.english.model.BotAction.EXIT;

@Component
public class AddWordWithoutBotRequestExampleActionHandler implements BotActionHandler {
    @Override
    public BotAction getBotAction() {
        return BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_EXAMPLE;
    }

    @Override
    public List<SendMessage> processAction(UserState userState, String message) {
        if (userState.getUserStatus() != UserStatus.FILLING_TRANSLATION_WITHOUT_BOT) {
            throw new BadUserStatusException(getBotAction(), userState.getUserStatus());
        }
        if (StringUtils.isBlank(message)) {
            throw new BadActionSentMessageException("Sent translation is empty");
        }
        userState.getCurrentWordState().setTranslation(message);
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE_WITHOUT_BOT);
        return List.of(sendWithActionButtons(userState.getUserId(),
                String.format(ADD_WORD_WITHOUT_BOT_REQUEST_EXAMPLE.getAnswerMessage(),
                        userState.getCurrentWordState().getOriginal(),
                        userState.getCurrentWordState().getTranslation()),
                List.of(EXIT), 1));
    }
}
