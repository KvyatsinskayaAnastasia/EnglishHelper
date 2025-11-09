package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RepeatingRandomActionHandler implements BotActionHandler {
    private static final Integer ONE_REPEATING_ITERATION_SIZE = 10;
    private static final String NO_WORDS_MESSAGE = "Что бы что-то повторить, нужно что-то добавить";
    private static final String LET_REPEAT_PREFIX = "Давай повторять вот эти слова:\n\n";
    private static final String WORD_INFO_FORMAT = "%s: %s.%nПример: %s";

    private final WordService wordService;

    @Override
    public BotAction getBotAction() {
        return BotAction.REPEATING_RANDOM;
    }

    @Override
    public SendMessage processAction(UserState userState, String message) {
        var words = wordService.getRandomWordEOSByUserId(userState.getUserId(), ONE_REPEATING_ITERATION_SIZE);

        if (CollectionUtils.isEmpty(words)) {
            return createNoWordsResponse(userState.getUserId());
        }

        userState.setRepeatingState(new RepeatingState(new ArrayList<>(words),
                null, null, false));
        return createWordsResponse(userState.getUserId(), words);
    }

    private SendMessage createNoWordsResponse(Long userId) {
        return sendWithActionButtons(userId, NO_WORDS_MESSAGE, ADD_OPTIONS, 2);
    }

    private SendMessage createWordsResponse(Long userId, List<WordForRepeat> words) {
        String messageText = formatWordsMessage(words);
        return sendWithActionButtons(userId, messageText, REPEATING_OPTIONS, 1);
    }

    private String formatWordsMessage(List<WordForRepeat> words) {
        return LET_REPEAT_PREFIX + words.stream()
                .map(this::formatWord)
                .collect(Collectors.joining("\n\n"));
    }

    private String formatWord(WordForRepeat word) {
        return String.format(WORD_INFO_FORMAT, word.getOriginal(), word.getTranslation(), word.getExampleSentence());
    }
}
