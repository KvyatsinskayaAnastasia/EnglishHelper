package com.learn.english.handler.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RepeatingPlannedActionHandler implements BotActionHandler {
    private final WordService wordService;

    private final static Integer SHOW_REPEATING_SIZE = 10;

    @Override
    public BotAction getBotAction() {
        return BotAction.REPEATING_PLANNED;
    }

    @Override
    public List<SendMessage> processAction(UserState userState, String message) {
        var words = wordService.getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(userState.getUserId(), SHOW_REPEATING_SIZE);
        if (!CollectionUtils.isEmpty(words)) {
            userState.setRepeatingState(new RepeatingState(new ArrayList<>(words), null, null, true));
        }
        return List.of(CollectionUtils.isEmpty(words)
                ? sendWithActionButtons(userState.getUserId(),
                "Все слова повторены!", BASE_OPTIONS, 2)
                : sendWithActionButtons(userState.getUserId(), "Давай повторять вот эти слова: \n\n" +
                words.stream()
                        .map(word -> String.format("%s: %s.\nПример: %s", word.getOriginal(),
                                word.getTranslation(), word.getExampleSentence()))
                        .collect(Collectors.joining("\n\n")), REPEATING_OPTIONS, 1));
    }
}
