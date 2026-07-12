package com.learn.english.action.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.learn.english.utils.ButtonUtils.sendWithActionButtons;

@Component
@RequiredArgsConstructor
public class ExitActionHandler implements BotActionHandler {
    private final TelegramHelperService telegramHelperService;
    private final UserStateService userStateService;

    @Override
    public BotAction getBotAction() {
        return BotAction.EXIT;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        userState.setCurrentWordState(null);
        userState.setProposesState(null);
        userState.setRepeatingState(null);
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        userState.setLastBotCommandMessageId(
                telegramHelperService.sendMessage(sendWithActionButtons(userState.getUserId(),
                        BotAction.EXIT.getAnswerMessage(), BASE_OPTIONS, 2)));
        userStateService.saveUserState(userState);
    }
}
