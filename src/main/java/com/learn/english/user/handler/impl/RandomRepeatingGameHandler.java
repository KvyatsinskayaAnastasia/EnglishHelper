package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.user.handler.UserStatusHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_ORIGINAL;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_TRANSLATION;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.model.BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;

@Component
@RequiredArgsConstructor
public class RandomRepeatingGameHandler implements UserStatusHandler {
    private static final List<BotAction> ENABLED_CHOOSE_RANDOM_REPEATING_BOT_ACTIONS = List.of(
            REPEATING_CHOOSE_TRANSLATION,
            REPEATING_CHOOSE_ORIGINAL,
            REPEATING_WRITE_WORD_BY_TRANSLATION,
            EXIT);


    private final Map<BotAction, BotActionHandler> botActionHandlers;
    private final TelegramHelperService telegramHelperService;
    private final UserStateService userStateService;

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.RANDOM_REPEATING_GAME;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        var botAction = getBotAction(update);
        if (botAction != null && ENABLED_CHOOSE_RANDOM_REPEATING_BOT_ACTIONS.contains(botAction)
                || userState.getRepeatingState() != null) {
            botActionHandlers.get(botAction != null ? botAction : userState.getRepeatingState().getChosenRepeatingGame())
                    .processAction(userState, update);
            userStateService.saveUserState(userState);
            return;
        }
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(sendWithActionButtons(userState.getUserId(),
                REPEATING_RANDOM.getAnswerMessage(), ENABLED_CHOOSE_RANDOM_REPEATING_BOT_ACTIONS, 2)));
        userStateService.saveUserState(userState);
    }
}
