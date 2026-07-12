package com.learn.english.action.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.utils.ButtonUtils.sendWithActionButtons;

@Component
@RequiredArgsConstructor
public class RepeatingPlannedHandler implements BotActionHandler {
    private static final Integer ONE_REPEATING_ITERATION_SIZE = 10;

    private final UserStateService userStateService;
    private final TelegramHelperService telegramHelperService;
    private final WordService wordService;

    @Override
    public BotAction getBotAction() {
        return REPEATING_PLANNED;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        var words = wordService.getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(userState.getUserId(), ONE_REPEATING_ITERATION_SIZE);
        if (!CollectionUtils.isEmpty(words)) {
            userState.setRepeatingState(new RepeatingState(new ArrayList<>(words), null, null, true, null));
        }
        userState.setUserStatus(UserStatus.PLANNED_REPEATING_GAME);
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(CollectionUtils.isEmpty(words)
                ? sendWithActionButtons(userState.getUserId(), "Все слова повторены!", BASE_OPTIONS, 2)
                : sendWithActionButtons(userState.getUserId(), "Давай повторять вот эти слова: \n\n" +
                words.stream()
                        .map(word -> String.format("%s: %s.\nПример: %s", word.getOriginal(),
                                word.getTranslation(), word.getExampleSentence()))
                        .collect(Collectors.joining("\n\n")), REPEATING_OPTIONS, 1)));
        userStateService.saveUserState(userState);
    }
}
