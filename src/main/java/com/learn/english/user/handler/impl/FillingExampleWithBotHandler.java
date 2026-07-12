package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordService;
import com.learn.english.user.handler.UserStatusHandler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

import static com.learn.english.model.BotAction.ADD_WORD_SAVE_WORD;
import static com.learn.english.model.BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;

@Component
@RequiredArgsConstructor
public class FillingExampleWithBotHandler implements UserStatusHandler {
    private static final List<BotAction> ENABLED_FILLING_EXAMPLE_WITH_BOT_BOT_ACTIONS = List.of(
            EXIT
    );
    private static final List<BotAction> BASE_OPTIONS = List.of(
            ADD_WORD_WITH_BOT_REQUEST_ORIGINAL,
            ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL,
            REPEATING_PLANNED,
            REPEATING_RANDOM
    );

    private final Map<BotAction, BotActionHandler> botActionHandlers;
    private final TelegramHelperService telegramHelperService;
    private final UserStateService userStateService;
    private final WordService wordService;

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.FILLING_EXAMPLE_WITH_BOT;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        var botAction = getBotAction(update);
        if (botAction != null && ENABLED_FILLING_EXAMPLE_WITH_BOT_BOT_ACTIONS.contains(botAction)) {
            botActionHandlers.get(botAction).processAction(userState, update);
            return;
        }
        var example = update.getCallbackQuery() != null
                ? update.getCallbackQuery().getData()
                : update.getMessage().getText();
        var wordState = userState.getCurrentWordState();
        if (userState.getProposesState() != null
                && CollectionUtils.isNotEmpty(userState.getProposesState().getExamples())
                && StringUtils.isNumeric(example)) {
            var numericAnswer = Integer.parseInt(example);
            var examples = userState.getProposesState().getExamples();
            if (examples.size() >= numericAnswer) {
                wordState.setExampleSentence(examples.get(numericAnswer - 1));
            }
        }
        if (StringUtils.isBlank(wordState.getExampleSentence())) {
            wordState.setExampleSentence(example);
        }
        wordService.addNewWord(wordState, userState.getUserId());
        userState.setCurrentWordState(null);
        userState.setProposesState(null);
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(sendWithActionButtons(userState.getUserId(),
                String.format(ADD_WORD_SAVE_WORD.getAnswerMessage(), wordState.getOriginal(),
                        wordState.getTranslation(), wordState.getExampleSentence()),
                BASE_OPTIONS, 2)));
        userStateService.saveUserState(userState);
    }
}
