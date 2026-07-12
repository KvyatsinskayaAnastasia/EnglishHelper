package com.learn.english.action.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.model.Icon;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.WordService;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.utils.ButtonUtils.sendWithActionButtons;
import static com.learn.english.utils.ButtonUtils.sendWithNumberButtons;

public abstract class RepeatingActionHandler implements BotActionHandler {
    protected static final int RANDOM_WORDS_COUNT = 3;
    protected static final int BUTTONS_PER_ROW = 1;
    protected static final int OPTIONS_BUTTONS_PER_ROW = 2;
    protected static final List<@NotNull UserStatus> GAME_CHOSEN_USER_STATUSES = List.of(
            UserStatus.REPEATING_CHOOSE_ORIGINAL,
            UserStatus.REPEATING_CHOOSE_TRANSLATION,
            UserStatus.REPEATING_WRITE_WORD_BY_TRANSLATION
    );

    protected final WordService wordService;
    protected final TelegramHelperService telegramHelperService;
    protected final Random random;

    protected RepeatingActionHandler(WordService wordService, TelegramHelperService telegramHelperService,
                                     Random random) {
        this.wordService = wordService;
        this.telegramHelperService = telegramHelperService;
        this.random = random;
    }

    protected abstract boolean isCorrectAnswer(UserState userState, String answer);

    protected abstract String getQuestionText(WordForRepeat currentWord);

    protected abstract String getRetryText(UserState userState, String message);

    protected abstract List<String> getRandomWords(UserState userState, WordForRepeat word);

    protected abstract String getCorrectAnswer(WordForRepeat word);

    @Override
    public void processAction(UserState userState, Update update) {
        if (userState.getRepeatingState().getChosenRepeatingGame() != null) {
            handleUserAnswer(userState, update);
        } else {
            userState.getRepeatingState().setChosenRepeatingGame(getBotAction());
        }
        handleNextWord(userState);
    }

    protected void handleUserAnswer(UserState userState, Update update) {
        try {
            var answer = getSelectedText(userState, update);
            if (isCorrectAnswer(userState, answer)) {
                handleCorrectAnswer(userState);
                handleNextWord(userState);
            } else {
                handleWrongAnswer(userState, update, answer);
            }
        } catch (NumberFormatException e) {
            handleInvalidInput(userState, getSelectedText(userState, update));
        }
    }

    protected boolean isCorrectAnswer(String answer, String correctAnswer) {
        return answer.equalsIgnoreCase(correctAnswer);
    }

    protected String getSelectedText(UserState userState, Update update) {
        return update.getCallbackQuery() != null
                ? userState.getRepeatingState().getRepeatingLabels()
                .get(Integer.parseInt(update.getCallbackQuery().getData()) - 1)
                : update.getMessage().getText();
    }

    protected void markWrongAnswer(UserState userState, Update update) {
        if (update.getCallbackQuery() != null) {
            var selectedIndex = Integer.parseInt(update.getCallbackQuery().getData()) - 1;
            var selectedText = userState.getRepeatingState()
                    .getRepeatingLabels()
                    .get(selectedIndex);
            userState.getRepeatingState()
                    .getRepeatingLabels().set(selectedIndex, selectedText.concat(" " + Icon.NOT.get()));
        } else {
            var selectedText = update.getMessage().getText();
            var selectedIndex = userState.getRepeatingState().getRepeatingLabels().indexOf(selectedText);
            if (selectedIndex != -1) {
                userState.getRepeatingState()
                        .getRepeatingLabels().set(selectedIndex, selectedText.concat(" " + Icon.NOT.get()));
            }
        }
    }

    protected void handleCorrectAnswer(UserState userState) {
        RepeatingState repeatingState = userState.getRepeatingState();
        WordForRepeat currentWord = repeatingState.getCurrentRepeatingWord();

        wordService.updateRepeatedWord(currentWord.getId(), currentWord.isRepeatFailed());
        repeatingState.getRepeatingWords().remove(currentWord);

        telegramHelperService.sendMessage(SendMessage.builder()
                .chatId(userState.getUserId())
                .text(String.format("Правильно!\nСлово: %s\nПеревод: %s", currentWord.getOriginal(),
                        currentWord.getTranslation()))
                .build());
    }

    protected void handleWrongAnswer(UserState userState, Update update, String answer) {
        userState.getRepeatingState().getCurrentRepeatingWord().setRepeatFailed(true);
        markWrongAnswer(userState, update);
        userState.setLastBotCommandMessageId(
                telegramHelperService.sendMessage(sendWithNumberButtons(userState.getUserId(),
                        getRetryText(userState, answer),
                        userState.getRepeatingState().getRepeatingLabels(),
                        List.of(EXIT), BUTTONS_PER_ROW)));
    }

    protected void handleInvalidInput(UserState userState, String answer) {
        userState.setLastBotCommandMessageId(
                telegramHelperService.sendMessage(sendWithNumberButtons(userState.getUserId(),
                        "Ответ " + answer + " отсутствует. Пожалуйста, выберите вариант из предложенных: " +
                                getRetryText(userState, answer),
                        userState.getRepeatingState().getRepeatingLabels(),
                        List.of(EXIT),
                        BUTTONS_PER_ROW)));
    }

    protected void handleNextWord(UserState userState) {
        if (isAllWordsRepeated(userState)) {
            handleCompletion(userState);
        }
        prepareNextWord(userState);
        generateQuestionMessage(userState);
    }

    protected boolean isAllWordsRepeated(UserState userState) {
        return userState.getRepeatingState().getRepeatingWords().isEmpty();
    }

    protected void handleCompletion(UserState userState) {
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        userState.setRepeatingState(null);
        int countWordsForRepeat = wordService.countWordsForRepeat(userState.getUserId());
        String message = countWordsForRepeat > 0 ?
                String.format("Отлично! Осталось повторить: %d", countWordsForRepeat) :
                "Все слова повторены!";

        telegramHelperService.sendMessage(
                sendWithActionButtons(userState.getUserId(), message, BASE_OPTIONS, OPTIONS_BUTTONS_PER_ROW));
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

    protected void generateQuestionMessage(UserState userState) {
        String question = getQuestionText(userState.getRepeatingState().getCurrentRepeatingWord());
        userState.setLastBotCommandMessageId(
                telegramHelperService.sendMessage(sendWithNumberButtons(userState.getUserId(),
                        question,
                        userState.getRepeatingState().getRepeatingLabels(),
                        List.of(EXIT),
                        BUTTONS_PER_ROW)));
    }
}
