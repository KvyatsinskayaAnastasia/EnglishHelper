package com.learn.english.action.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.utils.ButtonUtils.sendWithActionButtons;

@Component
@RequiredArgsConstructor
public class RepeatingRandomHandler implements BotActionHandler {
    private static final Integer ONE_REPEATING_ITERATION_SIZE = 10;
    private static final String NO_WORDS_MESSAGE = "Что бы что-то повторить, нужно что-то добавить";
    private static final String LET_REPEAT_PREFIX = "Давай повторять вот эти слова:\n\n";
    private static final String WORD_INFO_FORMAT = "%s: %s.%nПример: %s";

    private final UserStateService userStateService;
    private final TelegramHelperService telegramHelperService;
    private final WordService wordService;

    @Override
    public BotAction getBotAction() {
        return REPEATING_RANDOM;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        var words = wordService.getRandomWordEOSByUserId(userState.getUserId(), ONE_REPEATING_ITERATION_SIZE);
        if (CollectionUtils.isEmpty(words)) {
            userState.setLastBotCommandMessageId(
                    telegramHelperService.sendMessage(sendWithActionButtons(userState.getUserId(),
                            NO_WORDS_MESSAGE, ADD_OPTIONS, 2)));
            userStateService.saveUserState(userState);
            return;
        }
        userState.setRepeatingState(new RepeatingState(new ArrayList<>(words), null,
                null, false,  null));
        userState.setUserStatus(UserStatus.RANDOM_REPEATING_GAME);
        userState.setLastBotCommandMessageId(
                telegramHelperService.sendMessage(sendWithActionButtons(userState.getUserId(),
                        formatWordsMessage(words), REPEATING_OPTIONS, 1)));
        userStateService.saveUserState(userState);
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
