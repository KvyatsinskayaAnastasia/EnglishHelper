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
public class RepeatingChooseTranslationActionHandler extends RepeatingActionHandler {

    public RepeatingChooseTranslationActionHandler(WordService wordService, TelegramHelperService telegramHelperService,
                                                   Random random) {
        super(wordService, telegramHelperService, random);
    }

    public BotAction getBotAction() {
        return BotAction.REPEATING_CHOOSE_TRANSLATION;
    }

    @Override
    protected boolean isCorrectAnswer(UserState userState, String answer) {
        return isCorrectAnswer(answer,
                userState.getRepeatingState().getCurrentRepeatingWord().getTranslation());
    }

    @Override
    protected String getQuestionText(WordForRepeat currentWord) {
        return "Выберите перевод слова: " + currentWord.getOriginal() + "\n";
    }

    @Override
    protected String getRetryText(UserState userState, String answer) {
        return "Не правильный вариант: " + answer
                + ".\nПопробуйте еще раз!\nВыберете перевод слова: " + userState.getRepeatingState().getCurrentRepeatingWord().getOriginal() + "\n";
    }

    @Override
    protected List<String> getRandomWords(UserState userState, WordForRepeat word) {
        return wordService.getRandomTranslationsByUserId(
                userState.getUserId(),
                word.getId(),
                RANDOM_WORDS_COUNT);
    }

    @Override
    protected String getCorrectAnswer(WordForRepeat word) {
        return word.getTranslation();
    }
}