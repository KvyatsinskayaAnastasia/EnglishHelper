package com.learn.english.action.handler.impl;

import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.WordService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class RepeatingChooseOriginalActionHandler extends RepeatingActionHandler {
    public RepeatingChooseOriginalActionHandler(WordService wordService, TelegramHelperService telegramHelperService,
                                                Random random) {
        super(wordService, telegramHelperService, random);
    }

    public BotAction getBotAction() {
        return BotAction.REPEATING_CHOOSE_ORIGINAL;
    }

    @Override
    protected boolean isCorrectAnswer(UserState userState, String answer) {
        return isCorrectAnswer(answer,
                userState.getRepeatingState().getCurrentRepeatingWord().getOriginal());
    }

    @Override
    protected String getQuestionText(WordForRepeat currentWord) {
        return "Выберите слово, которое переводится как: " + currentWord.getTranslation() + "\n";
    }

    @Override
    protected String getRetryText(UserState userState, String answer) {
        return "Не правильный вариант: " + answer
                + ".\nПопробуйте еще раз!\nВыберете слово, которое переводится как: " + userState.getRepeatingState().getCurrentRepeatingWord().getTranslation() + "\n";
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
