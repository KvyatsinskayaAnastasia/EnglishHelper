package com.learn.english.action.handler;

import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_ORIGINAL;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_TRANSLATION;
import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.model.BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;

public interface BotActionHandler {
    List<BotAction> BASE_OPTIONS = List.of(
            ADD_WORD_WITH_BOT_REQUEST_ORIGINAL,
            ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL,
            REPEATING_PLANNED,
            REPEATING_RANDOM);
    List<BotAction> REPEATING_OPTIONS = List.of(
            REPEATING_CHOOSE_TRANSLATION,
            REPEATING_CHOOSE_ORIGINAL,
            REPEATING_WRITE_WORD_BY_TRANSLATION,
            EXIT);
    List<BotAction> ADD_OPTIONS = List.of(
            ADD_WORD_WITH_BOT_REQUEST_ORIGINAL,
            ADD_WORD_WITHOUT_BOT_REQUEST_ORIGINAL);

    BotAction getBotAction();

    void processAction(UserState userState, Update update);
}
