package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.user.handler.UserStatusHandler;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

import static com.learn.english.action.handler.BotActionHandler.REPEATING_OPTIONS;
import static com.learn.english.model.BotAction.HELP;

abstract class RepeatingGameStatusHandler implements UserStatusHandler {
    private static final int MENU_BUTTONS_PER_ROW = 2;

    private final Map<BotAction, BotActionHandler> botActionHandlers;
    private final TelegramHelperService telegramHelperService;
    private final UserStateService userStateService;

    protected RepeatingGameStatusHandler(Map<BotAction, BotActionHandler> botActionHandlers,
                                         TelegramHelperService telegramHelperService,
                                         UserStateService userStateService) {
        this.botActionHandlers = botActionHandlers;
        this.telegramHelperService = telegramHelperService;
        this.userStateService = userStateService;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        BotAction botAction = getBotAction(update);
        if (botAction != null && REPEATING_OPTIONS.contains(botAction)) {
            delegate(botAction, userState, update);
            return;
        }
        BotAction chosenGame = chosenRepeatingGame(userState);
        if (botAction == null && chosenGame != null) {
            delegate(chosenGame, userState, update);
            return;
        }
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                sendWithActionButtons(userState.getUserId(), HELP.getAnswerMessage(),
                        REPEATING_OPTIONS, MENU_BUTTONS_PER_ROW)));
        userStateService.saveUserState(userState);
    }

    private void delegate(BotAction action, UserState userState, Update update) {
        try {
            botActionHandlers.get(action).processAction(userState, update);
        } finally {
            userStateService.saveUserState(userState);
        }
    }

    private static BotAction chosenRepeatingGame(UserState userState) {
        RepeatingState repeatingState = userState.getRepeatingState();
        return repeatingState == null ? null : repeatingState.getChosenRepeatingGame();
    }
}
