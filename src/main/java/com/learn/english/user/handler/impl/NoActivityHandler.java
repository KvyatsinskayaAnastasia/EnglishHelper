package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.service.UserStateService;
import com.learn.english.user.handler.UserStatusHandler;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

import static com.learn.english.model.BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.HELP;
import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;

@Component
@RequiredArgsConstructor
public class NoActivityHandler implements UserStatusHandler {
    private static final List<BotAction> ENABLED_NO_ACTIVITY_BOT_ACTIONS = List.of(
            ADD_WORD_WITH_BOT_REQUEST_ORIGINAL,
            ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL,
            REPEATING_PLANNED,
            REPEATING_RANDOM
    );

    private final Map<BotAction, BotActionHandler> botActionHandlers;
    private final TelegramHelperService telegramHelperService;
    private final UserStateService userStateService;

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.NO_ACTIVITY;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        var botAction = getBotAction(update);
        if (botAction != null && ENABLED_NO_ACTIVITY_BOT_ACTIONS.contains(botAction)) {
            botActionHandlers.get(botAction).processAction(userState, update);
        } else {
            userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                    sendWithActionButtons(userState.getUserId(), HELP.getAnswerMessage(),
                            ENABLED_NO_ACTIVITY_BOT_ACTIONS, 2)));
            userStateService.saveUserState(userState);
        }
    }
}
