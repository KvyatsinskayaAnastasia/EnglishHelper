package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.OllamaService;
import lombok.RequiredArgsConstructor;
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
    public SendMessage processAction(UserState userState, String message) {
        userState.getCurrentWordState().setTranslation(StringUtils.isNumeric(message)
                ? userState.getProposesState().getTranslations().get(Integer.parseInt(message) - 1) : message);
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE);
        userState.getProposesState().setExamples(
                ollamaService.proposeExamples(userState.getCurrentWordState().getOriginal(),
                        userState.getCurrentWordState().getTranslation())
        );
        return userState.getProposesState().getExamples() != null
                ? sendWithNumberButtons(userState.getUserId(), ADD_WORD_WITH_BOT_REQUEST_EXAMPLE.getMessage(), userState.getProposesState().getExamples(), List.of(EXIT, REGENERATE_EXAMPLES), 2)
                : sendWithActionButtons(userState.getUserId(), "Бот ничего не придумал, введи пример ручками или перегенерь", List.of(EXIT, REGENERATE_EXAMPLES), 2);
    }
}
