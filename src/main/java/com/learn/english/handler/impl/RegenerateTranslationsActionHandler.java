package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.service.OllamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_TRANSLATION;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REGENERATE_TRANSLATIONS;

@Component
@RequiredArgsConstructor
public class RegenerateTranslationsActionHandler implements BotActionHandler {
    private final OllamaService ollamaService;

    @Override
    public BotAction getBotAction() {
        return BotAction.REGENERATE_TRANSLATIONS;
    }

    @Override
    public SendMessage processAction(UserState userState, String message) {
        userState.getProposesState().setTranslations(ollamaService.proposeTranslations(userState.getCurrentWordState().getOriginal()));
        return userState.getProposesState().getTranslations() != null
                ? sendWithNumberButtons(userState.getUserId(), ADD_WORD_WITH_BOT_REQUEST_TRANSLATION.getMessage(), userState.getProposesState().getTranslations(), List.of(EXIT, REGENERATE_TRANSLATIONS), 2)
                : sendWithActionButtons(userState.getUserId(), "Бот ничего не придумал, введи перевод ручками или перегенерь", List.of(EXIT, REGENERATE_TRANSLATIONS), 2);
    }
}
