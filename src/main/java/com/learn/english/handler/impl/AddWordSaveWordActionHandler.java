package com.learn.english.handler.impl;

import com.learn.english.exception.BadActionSentMessageException;
import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;
import java.util.Set;

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
    public List<SendMessage> processAction(UserState userState, String message) {
        if (!Set.of(UserStatus.FILLING_EXAMPLE_WITHOUT_BOT, UserStatus.FILLING_EXAMPLE_WITH_BOT)
                .contains(userState.getUserStatus())) {
            throw new BadUserStatusException(getBotAction(), userState.getUserStatus());
        }
        if (StringUtils.isBlank(message)) {
            throw new BadActionSentMessageException("Sent example is empty");
        }
        var wordState = userState.getCurrentWordState();
        if (userState.getProposesState() != null
                && CollectionUtils.isNotEmpty(userState.getProposesState().getExamples())
                && StringUtils.isNumeric(message)) {
            var numericAnswer = Integer.parseInt(message);
            var examples = userState.getProposesState().getExamples();
            if (examples.size() >= numericAnswer) {
                wordState.setExampleSentence(examples.get(numericAnswer - 1));
            }
        }
        if (StringUtils.isBlank(wordState.getExampleSentence())) {
            wordState.setExampleSentence(message);
        }
        wordService.addNewWord(wordState, userState.getUserId());
        userState.setCurrentWordState(null);
        userState.setProposesState(null);
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        return List.of(sendWithActionButtons(userState.getUserId(),
                String.format(ADD_WORD_SAVE_WORD.getAnswerMessage(), wordState.getOriginal(),
                        wordState.getTranslation(), wordState.getExampleSentence()),
                BASE_OPTIONS, 2));
    }
}
