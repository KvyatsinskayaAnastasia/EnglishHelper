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

import static com.learn.english.model.BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_TRANSLATION;
import static com.learn.english.model.BotAction.EXIT;

@Component
@RequiredArgsConstructor
public class FillingOriginalWithoutBotHandler implements UserStatusHandler {
    private static final List<BotAction> ENABLED_FILLING_ORIGINAL_WITHOUT_BOT_BOT_ACTIONS = List.of(
            EXIT
    );

    private final Map<BotAction, BotActionHandler> botActionHandlers;
    private final TelegramHelperService telegramHelperService;
    private final UserStateService userStateService;

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.FILLING_ORIGINAL_WITHOUT_BOT;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        var botAction = getBotAction(update);
        if (botAction != null && ENABLED_FILLING_ORIGINAL_WITHOUT_BOT_BOT_ACTIONS.contains(botAction)) {
            botActionHandlers.get(botAction).processAction(userState, update);
            return;
        }
        userState.getCurrentWordState().setOriginal(update.getMessage().getText());
        userState.setUserStatus(UserStatus.FILLING_TRANSLATION_WITHOUT_BOT);
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(sendWithActionButtons(userState.getUserId(),
                String.format(ADD_WORD_WITHOUT_BOT_REQUEST_TRANSLATION.getAnswerMessage(),
                        userState.getCurrentWordState().getOriginal()),
                List.of(EXIT), 1)));
        userStateService.saveUserState(userState);
    }
}
