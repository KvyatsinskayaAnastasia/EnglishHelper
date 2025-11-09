package com.learn.english.handler;

import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.utils.ButtonUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_ORIGINAL;
import static com.learn.english.model.BotAction.REPEATING_CHOSE_TRANSLATION;
import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.model.BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;

public interface BotActionHandler {
    List<BotAction> BASE_OPTIONS = List.of(ADD_WORD_WITH_BOT_REQUEST_ORIGINAL, ADD_WORD_REQUEST_ORIGINAL, REPEATING_PLANNED, REPEATING_RANDOM);
    List<BotAction> ADD_OPTIONS = List.of(ADD_WORD_WITH_BOT_REQUEST_ORIGINAL, ADD_WORD_REQUEST_ORIGINAL);
    List<BotAction> CHOSE_REPEATING_OPTIONS = List.of(REPEATING_PLANNED, REPEATING_RANDOM);
    List<BotAction> REPEATING_OPTIONS = List.of(REPEATING_CHOSE_TRANSLATION,
            REPEATING_CHOOSE_ORIGINAL, REPEATING_WRITE_WORD_BY_TRANSLATION, EXIT);

    BotAction getBotAction();
    SendMessage processAction(UserState userState, String message);

    default SendMessage sendWithNumberButtons(Long userId, String message, List<String> buttonLabels,
                                              List<BotAction> botActions, int actionRowSize) {
        return ButtonUtils.sendWithNumberButtons(userId, message, buttonLabels, botActions, actionRowSize);
    }

    default SendMessage sendWithActionButtons(Long userId, String text, List<BotAction> botActions, int rowSize) {
        return ButtonUtils.sendWithActionButtons(userId, text, botActions, rowSize);
    }
}
