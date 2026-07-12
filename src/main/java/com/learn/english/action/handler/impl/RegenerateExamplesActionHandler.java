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
import static com.learn.english.model.BotAction.REGENERATE_EXAMPLES;
import static com.learn.english.utils.ButtonUtils.sendWithNumberButtons;

@Component
@RequiredArgsConstructor
public class RegenerateExamplesActionHandler implements BotActionHandler {
    private final OllamaService ollamaService;
    private final TelegramHelperService telegramHelperService;

    @Override
    public BotAction getBotAction() {
        return BotAction.REGENERATE_EXAMPLES;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        userState.getProposesState().setExamples(
                ollamaService.proposeExamples(userState.getCurrentWordState().getOriginal(),
                        userState.getCurrentWordState().getTranslation())
        );
        userState.setLastBotCommandMessageId(
                telegramHelperService.sendMessage(sendWithNumberButtons(userState.getUserId(),
                        String.format(REGENERATE_EXAMPLES.getAnswerMessage(),
                                userState.getCurrentWordState().getOriginal(),
                                userState.getCurrentWordState().getTranslation()),
                        userState.getProposesState().getExamples(),
                        List.of(EXIT, REGENERATE_EXAMPLES), 2)));
    }
}
