package com.learn.english.action.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.WordService;
import com.learn.english.utils.TextInput;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.utils.ButtonUtils.sendWithActionButtons;
import static com.learn.english.utils.ButtonUtils.sendWithNumberButtons;

public abstract class RepeatingActionHandler implements BotActionHandler {
    protected static final int RANDOM_WORDS_COUNT = 3;
    protected static final int BUTTONS_PER_ROW = 1;
    protected static final int OPTIONS_BUTTONS_PER_ROW = 2;

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

    protected abstract List<String> getRandomWords(UserState userState, WordForRepeat word);

    protected abstract String getCorrectAnswer(WordForRepeat word);

    @Override
    public void processAction(UserState userState, Update update) {
        if (userState.getRepeatingState().getChosenRepeatingGame() != null) {
            handleUserAnswer(userState, update);
        } else {
            userState.getRepeatingState().setChosenRepeatingGame(getBotAction());
            handleNextWord(userState);
        }
    }

    protected Integer freeTextLimit() {
        return null;
    }

    protected void handleUserAnswer(UserState userState, Update update) {
        if (TextInput.isStickerOrPhoto(update)) {
            askAgain(userState, TextInput.MEDIA_REJECTED);
            return;
        }
        String freeText = TextInput.value(update);
        Integer limit = freeTextLimit();
        if (freeText != null && limit != null && TextInput.exceeds(freeText, limit)) {
            askAgain(userState, TextInput.tooLong(limit));
            return;
        }
        try {
            var answer = getSelectedText(userState, update);
            if (answer == null || answer.isBlank()) {
                askAgain(userState, TextInput.TEXT_REQUIRED);
                return;
            }
            if (isCorrectAnswer(userState, answer)) {
                handleCorrectAnswer(userState);
                handleNextWord(userState);
            } else {
                handleWrongAnswer(userState, answer);
                handleNextWordAfterMiss(userState);
            }
        } catch (NumberFormatException e) {
            String rawAnswer = update.getCallbackQuery() != null
                    ? update.getCallbackQuery().getData()
                    : update.getMessage().getText();
            handleInvalidInput(userState, rawAnswer);
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

    protected void handleCorrectAnswer(UserState userState) {
        RepeatingState repeatingState = userState.getRepeatingState();
        WordForRepeat currentWord = repeatingState.getCurrentRepeatingWord();

        if (repeatingState.isIncreaseRepeatingCount()) {
            wordService.updateRepeatedWord(currentWord.getId(), currentWord.isRepeatFailed());
        }
        repeatingState.getRepeatingWords().removeIf(word -> Objects.equals(currentWord.getId(), word.getId()));
        sendText(userState, "Правильно!\n" + wordAndTranslation(currentWord));
    }

    protected void handleWrongAnswer(UserState userState, String answer) {
        WordForRepeat missedWord = userState.getRepeatingState().getCurrentRepeatingWord();
        missedWord.setRepeatFailed(true);
        sendText(userState, "Неправильно: " + answer + "\n" + wordAndTranslation(missedWord));
    }

    private void sendText(UserState userState, String text) {
        telegramHelperService.sendMessage(SendMessage.builder()
                .chatId(userState.getUserId())
                .text(text)
                .build());
    }

    private static String wordAndTranslation(WordForRepeat word) {
        return "Слово: " + word.getOriginal() + "\nПеревод: " + word.getTranslation();
    }

    protected void askAgain(UserState userState, String reason) {
        String prompt = reason + "\n" + getQuestionText(userState.getRepeatingState().getCurrentRepeatingWord());
        List<String> labels = userState.getRepeatingState().getRepeatingLabels();
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                labels == null || labels.isEmpty()
                        ? sendWithActionButtons(userState.getUserId(), prompt, List.of(EXIT), BUTTONS_PER_ROW)
                        : sendWithNumberButtons(userState.getUserId(), prompt, labels, List.of(EXIT), BUTTONS_PER_ROW)));
    }

    protected void handleInvalidInput(UserState userState, String answer) {
        askAgain(userState, "Ответ " + answer + " отсутствует. Пожалуйста, выберите вариант из предложенных:");
    }

    protected void handleNextWord(UserState userState) {
        askNextWord(userState, userState.getRepeatingState().getRepeatingWords());
    }

    private void handleNextWordAfterMiss(UserState userState) {
        askNextWord(userState, userState.getRepeatingState().candidatesExceptCurrent());
    }

    private void askNextWord(UserState userState, List<WordForRepeat> candidates) {
        if (isAllWordsRepeated(userState)) {
            handleCompletion(userState);
            return;
        }
        RepeatingState repeatingState = userState.getRepeatingState();
        WordForRepeat nextWord = getRandomWord(candidates);
        repeatingState.setCurrentRepeatingWord(nextWord);
        repeatingState.setRepeatingLabels(prepareWords(userState, nextWord));
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

        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                sendWithActionButtons(userState.getUserId(), message, BASE_OPTIONS, OPTIONS_BUTTONS_PER_ROW)));
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
