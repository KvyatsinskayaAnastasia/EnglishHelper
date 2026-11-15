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

import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.utils.ButtonUtils.sendWithActionButtons;

@Component
@RequiredArgsConstructor
public class RepeatingPlannedHandler implements BotActionHandler {
    private static final int ONE_REPEATING_ITERATION_SIZE = 10;
    private static final int MENU_BUTTONS_PER_ROW = 2;
    private static final int GAME_BUTTONS_PER_ROW = 1;
    private static final String ALL_REPEATED_MESSAGE = "Все слова повторены!";
    private static final String LET_REPEAT_PREFIX = "Давай повторять вот эти слова: \n\n";
    private static final String WORD_INFO_FORMAT = "%s: %s.\nПример: %s";

    private final UserStateService userStateService;
    private final TelegramHelperService telegramHelperService;
    private final WordService wordService;

    @Override
    public BotAction getBotAction() {
        return REPEATING_PLANNED;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        List<WordForRepeat> words = wordService.getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(
                userState.getUserId(), ONE_REPEATING_ITERATION_SIZE);
        if (CollectionUtils.isEmpty(words)) {
            showMainMenu(userState);
        } else {
            startPlannedSession(userState, words);
        }
        userStateService.saveUserState(userState);
    }

    private void showMainMenu(UserState userState) {
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                sendWithActionButtons(userState.getUserId(), ALL_REPEATED_MESSAGE, BASE_OPTIONS, MENU_BUTTONS_PER_ROW)));
    }

    private void startPlannedSession(UserState userState, List<WordForRepeat> words) {
        userState.setRepeatingState(new RepeatingState(new ArrayList<>(words), null, null, true, null));
        userState.setUserStatus(UserStatus.PLANNED_REPEATING_GAME);
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                sendWithActionButtons(userState.getUserId(), formatWordsMessage(words),
                        REPEATING_OPTIONS, GAME_BUTTONS_PER_ROW)));
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
