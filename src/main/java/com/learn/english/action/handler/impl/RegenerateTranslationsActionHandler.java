package com.learn.english.action.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.service.OllamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REGENERATE_TRANSLATIONS;
import static com.learn.english.utils.ButtonUtils.sendWithNumberButtons;

@Component
@RequiredArgsConstructor
public class RegenerateTranslationsActionHandler implements BotActionHandler {
    private final OllamaService ollamaService;
    private final TelegramHelperService telegramHelperService;

    @Override
    public BotAction getBotAction() {
        return BotAction.REGENERATE_TRANSLATIONS;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        userState.getProposesState().setTranslations(ollamaService
                .proposeTranslations(userState.getCurrentWordState().getOriginal()));
        userState.setLastBotCommandMessageId(
                telegramHelperService.sendMessage(sendWithNumberButtons(userState.getUserId(),
                        String.format(REGENERATE_TRANSLATIONS.getAnswerMessage(),
                                userState.getCurrentWordState().getOriginal()),
                        userState.getProposesState().getTranslations(),
                        List.of(EXIT, REGENERATE_TRANSLATIONS), 2)));
    }
}
