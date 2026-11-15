package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlannedRepeatingGameHandler extends RepeatingGameStatusHandler {

    public PlannedRepeatingGameHandler(Map<BotAction, BotActionHandler> botActionHandlers,
                                       TelegramHelperService telegramHelperService,
                                       UserStateService userStateService) {
        super(botActionHandlers, telegramHelperService, userStateService);
    }

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.PLANNED_REPEATING_GAME;
    }
}
