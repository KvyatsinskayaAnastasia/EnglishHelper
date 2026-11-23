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

import java.util.Map;

import static com.learn.english.action.handler.BotActionHandler.BASE_OPTIONS;
import static com.learn.english.model.BotAction.HELP;

@Component
@RequiredArgsConstructor
public class NoActivityHandler implements UserStatusHandler {
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
        if (botAction != null && BASE_OPTIONS.contains(botAction)) {
            botActionHandlers.get(botAction).processAction(userState, update);
        } else {
            userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                    sendWithActionButtons(userState.getUserId(), HELP.getAnswerMessage(),
                            BASE_OPTIONS, 2)));
            userStateService.saveUserState(userState);
        }
    }
}
