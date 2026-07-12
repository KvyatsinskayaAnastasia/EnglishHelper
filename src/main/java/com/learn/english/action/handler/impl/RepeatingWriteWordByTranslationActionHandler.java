package com.learn.english.action.handler.impl;

import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.WordService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.utils.ButtonUtils.sendWithActionButtons;

@Component
public class RepeatingWriteWordByTranslationActionHandler extends RepeatingActionHandler {

    public RepeatingWriteWordByTranslationActionHandler(WordService wordService, TelegramHelperService telegramHelperService,
                                                        Random random) {
        super(wordService, telegramHelperService, random);
    }

    public BotAction getBotAction() {
        return BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;
    }

    @Override
    protected boolean isCorrectAnswer(UserState userState, String answer) {
        return answer.equalsIgnoreCase(userState.getRepeatingState().getCurrentRepeatingWord().getOriginal());
    }

    @Override
    protected String getQuestionText(WordForRepeat currentWord) {
        return "Введите слово, которое переводится как: " + currentWord.getTranslation();
    }

    @Override
    protected String getRetryText(UserState userState, String message) {
        var currentWord = userState.getRepeatingState().getCurrentRepeatingWord();
        return "Не правильный ответ: " + message + "\nПопробуйте еще!\nВведите слово, которое переводится как: " + currentWord.getTranslation()
                + ".\nподсказка: " + currentWord.getOriginal().substring(0,
                Math.min(currentWord.getCountOfAttempts(), currentWord.getOriginal().length()));
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
    protected void generateQuestionMessage(UserState userState) {
        userState.setLastBotCommandMessageId(
                telegramHelperService.sendMessage(sendWithActionButtons(userState.getUserId(),
                        getQuestionText(userState.getRepeatingState().getCurrentRepeatingWord()),
                        List.of(EXIT), BUTTONS_PER_ROW)));
    }

    @Override
    protected void handleWrongAnswer(UserState userState, Update update, String answer) {
        var currentRepeatingWord = userState.getRepeatingState().getCurrentRepeatingWord();
        currentRepeatingWord.setRepeatFailed(true);
        currentRepeatingWord.increaseCountOfAttempts();
        userState.setLastBotCommandMessageId(telegramHelperService
                .sendMessage(sendWithActionButtons(userState.getUserId(),
                        getRetryText(userState, answer),
                        List.of(EXIT), BUTTONS_PER_ROW)));
    }

    @Override
    protected List<String> prepareWords(UserState userState, WordForRepeat word) {
        return Collections.emptyList();
    }
}
