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

import static com.learn.english.model.BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_TRANSLATION;
import static com.learn.english.model.BotAction.EXIT;

@Component
public class AddWordWithoutBotRequestTranslationActionHandler implements BotActionHandler {
    @Override
    public BotAction getBotAction() {
        return BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_TRANSLATION;
    }

    @Override
    public List<SendMessage> processAction(UserState userState, String message) {
        if (userState.getUserStatus() != UserStatus.FILLING_ORIGINAL_WITHOUT_BOT) {
            throw new BadUserStatusException(getBotAction(), userState.getUserStatus());
        }
        if (StringUtils.isBlank(message)) {
            throw new BadActionSentMessageException("Sent original is empty");
        }
        userState.getCurrentWordState().setOriginal(message);
        userState.setUserStatus(UserStatus.FILLING_TRANSLATION_WITHOUT_BOT);
        return List.of(sendWithActionButtons(userState.getUserId(),
                String.format(ADD_WORD_WITHOUT_BOT_REQUEST_TRANSLATION.getAnswerMessage(),
                        userState.getCurrentWordState().getOriginal()),
                List.of(EXIT), 1));
    }
}
