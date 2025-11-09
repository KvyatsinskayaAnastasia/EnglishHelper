package com.learn.english.handler.impl;

import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.WordService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.learn.english.model.BotAction.EXIT;

@Component
public class RepeatingWriteWordByTranslationActionHandler extends RepeatingActionHandler {

    public RepeatingWriteWordByTranslationActionHandler(WordService wordService, Random random) {
        super(wordService, random);
    }

    @Override
    public BotAction getBotAction() {
        return BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;
    }

    @Override
    protected UserStatus getUserStatus() {
        return UserStatus.REPEATING_WRITE_WORD_BY_TRANSLATION;
    }

    @Override
    protected boolean isCorrectAnswer(UserState userState, String message) {
        return message != null && message.equalsIgnoreCase(
                userState.getRepeatingState().getCurrentRepeatingWord().getOriginal());
    }

    @Override
    protected String getQuestionText(WordForRepeat currentWord) {
        return "Слово переводится как: " + currentWord.getTranslation();
    }

    @Override
    protected String getRetryText(WordForRepeat currentWord) {
        return "Попробуйте еще! Слово: " + currentWord.getTranslation()
                + ", первые буквы: " + currentWord.getOriginal().substring(0, Math.min(3, currentWord.getOriginal().length()));
    }

    @Override
    protected List<String> getRandomWords(UserState userState, WordForRepeat word) {
        return Collections.emptyList();
    }

    @Override
    protected String getCorrectAnswer(WordForRepeat word) {
        return word.getOriginal();
    }

    @Override
    protected SendMessage generateQuestionMessage(UserState userState) {
        return sendWithActionButtons(userState.getUserId(),
                getQuestionText(userState.getRepeatingState().getCurrentRepeatingWord()),
                List.of(EXIT), BUTTONS_PER_ROW);
    }

    @Override
    protected SendMessage handleIncorrectAnswer(UserState userState) {
        userState.getRepeatingState().getCurrentRepeatingWord().setRepeatFailed(true);
        return sendWithActionButtons(userState.getUserId(),
                getRetryText(userState.getRepeatingState().getCurrentRepeatingWord()),
                List.of(EXIT), BUTTONS_PER_ROW);
    }

    @Override
    protected List<String> prepareWords(UserState userState, WordForRepeat word) {
        return Collections.emptyList();
    }
}
