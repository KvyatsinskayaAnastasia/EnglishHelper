package com.learn.english.action.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.WordState;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.UserStatus.FILLING_ORIGINAL_WITHOUT_BOT;
import static com.learn.english.utils.ButtonUtils.sendWithActionButtons;

@Component
@RequiredArgsConstructor
public class AddWordWithoutBotRequestOriginalHandler implements BotActionHandler {
    private final TelegramHelperService telegramHelperService;
    private final UserStateService userStateService;

    @Override
    public BotAction getBotAction() {
        return ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        userState.setCurrentWordState(new WordState());
        userState.setUserStatus(FILLING_ORIGINAL_WITHOUT_BOT);
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(sendWithActionButtons(
                userState.getUserId(), getBotAction().getAnswerMessage(), List.of(EXIT), 1)));
        userStateService.saveUserState(userState);
    }
}
