package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.Icon;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.WordService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static com.learn.english.model.BotAction.EXIT;

public abstract class RepeatingActionHandler implements BotActionHandler {
    protected static final int RANDOM_WORDS_COUNT = 3;
    protected static final int BUTTONS_PER_ROW = 1;
    protected static final int OPTIONS_BUTTONS_PER_ROW = 2;

    protected final WordService wordService;
    protected final Random random;

    protected RepeatingActionHandler(WordService wordService, Random random) {
        this.wordService = wordService;
        this.random = random;
    }

    protected abstract boolean isCorrectAnswer(UserState userState, String selectedText);

    protected abstract String getQuestionText(WordForRepeat currentWord);

    protected abstract String getRetryText(UserState userState, String message);

    protected abstract List<String> getRandomWords(UserState userState, WordForRepeat word);

    protected abstract String getCorrectAnswer(WordForRepeat word);

    @Override
    public List<SendMessage> processAction(UserState userState, String message) {
        if (userState.getRepeatingState() == null) {
            return List.of(handleInitialState(userState));
        }

        if (message != null) {
            return handleUserAnswer(userState, message);
        } else {
            userState.setUserStatus(getUserStatus());
        }

        return List.of(handleNextWord(userState));
    }

    protected abstract UserStatus getUserStatus();

    protected SendMessage handleInitialState(UserState userState) {
        return sendWithActionButtons(userState.getUserId(),
                "Какие слова хотите повторить?",
                CHOSE_REPEATING_OPTIONS,
                OPTIONS_BUTTONS_PER_ROW);
    }

    protected List<SendMessage> handleUserAnswer(UserState userState, String message) {
        try {
            if (isCorrectAnswer(userState, message)) {
                return List.of(handleCorrectAnswer(userState), handleNextWord(userState));
            } else {
                return List.of(handleWrongAnswer(userState, message));
            }
        } catch (NumberFormatException e) {
            return List.of(handleInvalidInput(userState, message));
        }
    }

    protected boolean isCorrectAnswerByIndex(UserState userState, String message, String correctAnswer) {
        return getSelectedText(userState, message).equalsIgnoreCase(correctAnswer);
    }

    protected String getSelectedText(UserState userState, String message) {
        var selectedIndex = Integer.parseInt(message) - 1;
        return userState.getRepeatingState()
                .getRepeatingLabels()
                .get(selectedIndex);
    }

    protected void markWrongAnswer(UserState userState, String message) {
        var selectedIndex = Integer.parseInt(message) - 1;
        var selectedText = userState.getRepeatingState()
                .getRepeatingLabels()
                .get(selectedIndex);
        userState.getRepeatingState()
                .getRepeatingLabels().set(selectedIndex, selectedText.concat(" " + Icon.NOT.get()));
    }

    protected SendMessage handleCorrectAnswer(UserState userState) {
        RepeatingState repeatingState = userState.getRepeatingState();
        WordForRepeat currentWord = repeatingState.getCurrentRepeatingWord();

        wordService.updateRepeatedWord(currentWord.getId(), currentWord.isRepeatFailed());
        repeatingState.getRepeatingWords().remove(currentWord);

        return SendMessage.builder()
                .chatId(userState.getUserId())
                .text(String.format("Правильно!\nСлово: %s\nПеревод: %s", currentWord.getOriginal(),
                        currentWord.getTranslation()))
                .build();
    }

    protected SendMessage handleWrongAnswer(UserState userState, String message) {
        userState.getRepeatingState().getCurrentRepeatingWord().setRepeatFailed(true);
        markWrongAnswer(userState, message);
        return sendWithNumberButtons(userState.getUserId(),
                getRetryText(userState, message),
                userState.getRepeatingState().getRepeatingLabels(),
                List.of(EXIT), BUTTONS_PER_ROW);
    }

    protected SendMessage handleInvalidInput(UserState userState, String message) {
        return sendWithNumberButtons(userState.getUserId(),
                "Ответ " + message + " отсутствует. Пожалуйста, выберите вариант из предложенных: " +
                        getRetryText(userState, message),
                userState.getRepeatingState().getRepeatingLabels(),
                List.of(EXIT),
                BUTTONS_PER_ROW);
    }

    protected SendMessage handleNextWord(UserState userState) {
        if (isAllWordsRepeated(userState)) {
            return handleCompletion(userState);
        }

        prepareNextWord(userState);
        return generateQuestionMessage(userState);
    }

    protected boolean isAllWordsRepeated(UserState userState) {
        return userState.getRepeatingState().getRepeatingWords().isEmpty();
    }

    protected SendMessage handleCompletion(UserState userState) {
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        userState.setRepeatingState(null);
        int countWordsForRepeat = wordService.countWordsForRepeat(userState.getUserId());
        String message = countWordsForRepeat > 0 ?
                String.format("Отлично! Осталось повторить: %d", countWordsForRepeat) :
                "Все слова повторены!";

        return sendWithActionButtons(userState.getUserId(), message, BASE_OPTIONS, OPTIONS_BUTTONS_PER_ROW);
    }

    protected void prepareNextWord(UserState userState) {
        RepeatingState repeatingState = userState.getRepeatingState();
        WordForRepeat nextWord = getRandomWord(repeatingState.getRepeatingWords());
        repeatingState.setCurrentRepeatingWord(nextWord);

        List<String> words = prepareWords(userState, nextWord);
        repeatingState.setRepeatingLabels(words);
    }

    protected WordForRepeat getRandomWord(List<WordForRepeat> words) {
        return words.get(random.nextInt(words.size()));
    }

    protected List<String> prepareWords(UserState userState, WordForRepeat word) {
        List<String> randomWords = getRandomWords(userState, word);

        List<String> allWords = new ArrayList<>(randomWords);
        allWords.add(getCorrectAnswer(word));
        allWords.sort(Comparator.naturalOrder());

        return allWords;
    }

    protected SendMessage generateQuestionMessage(UserState userState) {
        String question = getQuestionText(userState.getRepeatingState().getCurrentRepeatingWord());

        return sendWithNumberButtons(userState.getUserId(),
                question,
                userState.getRepeatingState().getRepeatingLabels(),
                List.of(EXIT),
                BUTTONS_PER_ROW);
    }
}
