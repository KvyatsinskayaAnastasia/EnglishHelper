package com.learn.english.handler.impl;

import com.learn.english.exception.BadActionSentMessageException;
import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.OllamaService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_EXAMPLE;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REGENERATE_EXAMPLES;

@Component
@RequiredArgsConstructor
public class AddWordWithBotRequestExampleActionHandler implements BotActionHandler {
    private final OllamaService ollamaService;

    @Override
    public BotAction getBotAction() {
        return BotAction.ADD_WORD_WITH_BOT_REQUEST_EXAMPLE;
    }

    @Override
    public List<SendMessage> processAction(UserState userState, String message) {
        if (userState.getUserStatus() != UserStatus.FILLING_TRANSLATION_WITH_BOT) {
            throw new BadUserStatusException(getBotAction(), userState.getUserStatus());
        }
        if (StringUtils.isBlank(message)) {
            throw new BadActionSentMessageException("Sent translation is empty");
        }
        if (userState.getProposesState() != null
                && CollectionUtils.isNotEmpty(userState.getProposesState().getTranslations())
                && StringUtils.isNumeric(message)) {
            var numericAnswer = Integer.parseInt(message);
            var translations = userState.getProposesState().getTranslations();
            if (translations.size() >= numericAnswer) {
                userState.getCurrentWordState().setTranslation(translations.get(numericAnswer - 1));
            }
        }
        if (StringUtils.isBlank(userState.getCurrentWordState().getTranslation())) {
            userState.getCurrentWordState().setTranslation(message);
        }
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE_WITH_BOT);
        userState.getProposesState().setExamples(
                ollamaService.proposeExamples(userState.getCurrentWordState().getOriginal(),
                        userState.getCurrentWordState().getTranslation())
        );
        return List.of(sendWithNumberButtons(userState.getUserId(),
                String.format(ADD_WORD_WITH_BOT_REQUEST_EXAMPLE.getAnswerMessage(),
                        userState.getCurrentWordState().getOriginal(),
                        userState.getCurrentWordState().getTranslation()),
                userState.getProposesState().getExamples(), List.of(EXIT, REGENERATE_EXAMPLES), 2));
    }
}
