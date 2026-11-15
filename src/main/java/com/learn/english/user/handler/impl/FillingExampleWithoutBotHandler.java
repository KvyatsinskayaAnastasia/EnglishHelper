package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordState;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordService;
import com.learn.english.utils.TextInput;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.Optional;

@Component
public class FillingExampleWithoutBotHandler extends FillingExampleStatusHandler {

    public FillingExampleWithoutBotHandler(Map<BotAction, BotActionHandler> botActionHandlers,
                                           TelegramHelperService telegramHelperService,
                                           UserStateService userStateService,
                                           WordService wordService) {
        super(botActionHandlers, telegramHelperService, userStateService, wordService);
    }

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.FILLING_EXAMPLE_WITHOUT_BOT;
    }

    @Override
    protected boolean takeExample(UserState userState, Update update) {
        WordState wordState = userState.getCurrentWordState();
        if (StringUtils.isNotBlank(wordState.getExampleSentence())) {
            return true;
        }
        Optional<String> example = acceptText(userState, update, TextInput.EXAMPLE_LIMIT);
        if (example.isEmpty()) {
            return false;
        }
        wordState.setExampleSentence(example.get());
        return true;
    }
}
