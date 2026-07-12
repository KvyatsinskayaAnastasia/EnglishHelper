package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.OllamaService;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.user.handler.UserStatusHandler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_EXAMPLE;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REGENERATE_EXAMPLES;

@Component
@RequiredArgsConstructor
public class FillingTranslationWithBotHandler implements UserStatusHandler {
    private static final List<BotAction> ENABLED_FILLING_TRANSLATION_WITH_BOT_BOT_ACTIONS = List.of(
            EXIT
    );

    private final Map<BotAction, BotActionHandler> botActionHandlers;
    private final TelegramHelperService telegramHelperService;
    private final OllamaService ollamaService;
    private final UserStateService userStateService;

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.FILLING_TRANSLATION_WITH_BOT;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        var botAction = getBotAction(update);
        if (botAction != null && ENABLED_FILLING_TRANSLATION_WITH_BOT_BOT_ACTIONS.contains(botAction)) {
            botActionHandlers.get(botAction).processAction(userState, update);
            return;
        }
        var translation = update.getCallbackQuery() != null
                ? update.getCallbackQuery().getData()
                : update.getMessage().getText();
        if (userState.getProposesState() != null
                && CollectionUtils.isNotEmpty(userState.getProposesState().getTranslations())
                && StringUtils.isNumeric(translation)) {
            var numericAnswer = Integer.parseInt(translation);
            var translations = userState.getProposesState().getTranslations();
            if (translations.size() >= numericAnswer) {
                userState.getCurrentWordState().setTranslation(translations.get(numericAnswer - 1));
            }
        }
        if (StringUtils.isBlank(userState.getCurrentWordState().getTranslation())) {
            userState.getCurrentWordState().setTranslation(translation);
        }
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE_WITH_BOT);
        userState.getProposesState().setExamples(
                ollamaService.proposeExamples(userState.getCurrentWordState().getOriginal(),
                        userState.getCurrentWordState().getTranslation())
        );
        userState.setLastBotCommandMessageId(
                telegramHelperService.sendMessage(sendWithNumberButtons(userState.getUserId(),
                        String.format(ADD_WORD_WITH_BOT_REQUEST_EXAMPLE.getAnswerMessage(),
                                userState.getCurrentWordState().getOriginal(),
                                userState.getCurrentWordState().getTranslation()),
                        userState.getProposesState().getExamples(), List.of(EXIT, REGENERATE_EXAMPLES), 2)));
        userStateService.saveUserState(userState);
    }
}
