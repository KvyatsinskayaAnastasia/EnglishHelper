package com.learn.english.handler.impl;

import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.WordService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class RepeatingChoseOriginalActionHandler extends RepeatingActionHandler {
    public RepeatingChoseOriginalActionHandler(WordService wordService, Random random) {
        super(wordService, random);
    }

    @Override
    public BotAction getBotAction() {
        return BotAction.REPEATING_CHOOSE_ORIGINAL;
    }

    @Override
    protected UserStatus getUserStatus() {
        return UserStatus.REPEATING_CHOOSE_ORIGINAL;
    }

    @Override
    protected boolean isCorrectAnswer(UserState userState, String message) {
        return isCorrectAnswerByIndex(userState, message,
                userState.getRepeatingState().getCurrentRepeatingWord().getOriginal());
    }

    @Override
    protected String getQuestionText(WordForRepeat currentWord) {
        return "Выберите слово, которое переводится как: " + currentWord.getTranslation() + "\n";
    }

    @Override
    protected String getRetryText(WordForRepeat currentWord) {
        return "Попробуйте еще раз: " + currentWord.getTranslation() + "\n";
    }

    @Override
    protected List<String> getRandomWords(UserState userState, WordForRepeat word) {
        return wordService.getRandomOriginalsByUserId(
                userState.getUserId(),
                word.getId(),
                RANDOM_WORDS_COUNT);
    }

    @Override
    protected String getCorrectAnswer(WordForRepeat word) {
        return word.getOriginal();
    }
}
