package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static com.learn.english.model.BotAction.ADD_WORD_SAVE_WORD;

@Component
@RequiredArgsConstructor
public class AddWordSaveWordActionHandler implements BotActionHandler {
    private final WordService wordService;

    @Override
    public BotAction getBotAction() {
        return BotAction.ADD_WORD_SAVE_WORD;
    }

    @Override
    public SendMessage processAction(UserState userState, String message) {
        userState.getCurrentWordState().setExampleSentence(StringUtils.isNumeric(message)
                ? userState.getProposesState().getExamples().get(Integer.parseInt(message) - 1) : message);
        wordService.addNewWord(userState.getCurrentWordState(), userState.getUserId());
        userState.setCurrentWordState(null);
        userState.setProposesState(null);
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        return sendWithActionButtons(userState.getUserId(), ADD_WORD_SAVE_WORD.getMessage(), BASE_OPTIONS, 2);
    }
}
