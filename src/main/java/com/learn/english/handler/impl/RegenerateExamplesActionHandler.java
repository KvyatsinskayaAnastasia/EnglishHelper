package com.learn.english.handler.impl;

import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.OllamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REGENERATE_EXAMPLES;

@Component
@RequiredArgsConstructor
public class RegenerateExamplesActionHandler implements BotActionHandler {
    private final OllamaService ollamaService;

    @Override
    public BotAction getBotAction() {
        return BotAction.REGENERATE_EXAMPLES;
    }

    @Override
    public List<SendMessage> processAction(UserState userState, String message) {
        if (UserStatus.FILLING_EXAMPLE_WITH_BOT != userState.getUserStatus()) {
            throw new BadUserStatusException(getBotAction(), userState.getUserStatus());
        }
        userState.getProposesState().setExamples(
                ollamaService.proposeExamples(userState.getCurrentWordState().getOriginal(),
                        userState.getCurrentWordState().getTranslation())
        );
        return List.of(sendWithNumberButtons(userState.getUserId(),
                String.format(REGENERATE_EXAMPLES.getAnswerMessage(),
                        userState.getCurrentWordState().getOriginal(),
                        userState.getCurrentWordState().getTranslation()),
                userState.getProposesState().getExamples(),
                List.of(EXIT, REGENERATE_EXAMPLES), 2));
    }
}
