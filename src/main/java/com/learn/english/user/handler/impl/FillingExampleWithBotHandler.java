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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

@Component
public class FillingExampleWithBotHandler extends FillingExampleStatusHandler {

    public FillingExampleWithBotHandler(Map<BotAction, BotActionHandler> botActionHandlers,
                                        TelegramHelperService telegramHelperService,
                                        UserStateService userStateService,
                                        WordService wordService) {
        super(botActionHandlers, telegramHelperService, userStateService, wordService);
    }

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.FILLING_EXAMPLE_WITH_BOT;
    }

    @Override
    protected boolean takeExample(UserState userState, Update update) {
        WordState wordState = userState.getCurrentWordState();
        String example = update.getCallbackQuery() != null
                ? update.getCallbackQuery().getData()
                : TextInput.value(update);
        applyProposal(userState, wordState, example);
        if (StringUtils.isNotBlank(wordState.getExampleSentence())) {
            return true;
        }
        if (example == null || TextInput.exceeds(example, TextInput.EXAMPLE_LIMIT)) {
            keepAsking(userState, example == null
                    ? TextInput.TEXT_REQUIRED
                    : TextInput.tooLong(TextInput.EXAMPLE_LIMIT));
            return false;
        }
        wordState.setExampleSentence(example);
        return true;
    }

    private static void applyProposal(UserState userState, WordState wordState, String example) {
        if (userState.getProposesState() == null
                || CollectionUtils.isEmpty(userState.getProposesState().getExamples())
                || !StringUtils.isNumeric(example)) {
            return;
        }
        int numericAnswer = Integer.parseInt(example);
        List<String> examples = userState.getProposesState().getExamples();
        if (examples.size() >= numericAnswer) {
            wordState.setExampleSentence(examples.get(numericAnswer - 1));
        }
    }
}
