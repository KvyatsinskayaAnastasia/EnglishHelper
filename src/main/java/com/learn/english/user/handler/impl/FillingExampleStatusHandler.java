package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.exception.DuplicateWordException;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordState;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordService;
import com.learn.english.user.handler.UserStatusHandler;
import com.learn.english.utils.TextInput;
import org.springframework.dao.DataIntegrityViolationException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.learn.english.action.handler.BotActionHandler.BASE_OPTIONS;
import static com.learn.english.model.BotAction.ADD_WORD_ALREADY_EXISTS;
import static com.learn.english.model.BotAction.ADD_WORD_SAVE_WORD;
import static com.learn.english.model.BotAction.EXIT;

abstract class FillingExampleStatusHandler implements UserStatusHandler {
    private static final List<BotAction> EXIT_ACTIONS = List.of(EXIT);
    private static final int MENU_BUTTONS_PER_ROW = 2;

    private final Map<BotAction, BotActionHandler> botActionHandlers;
    private final TelegramHelperService telegramHelperService;
    private final UserStateService userStateService;
    private final WordService wordService;

    protected FillingExampleStatusHandler(Map<BotAction, BotActionHandler> botActionHandlers,
                                          TelegramHelperService telegramHelperService,
                                          UserStateService userStateService,
                                          WordService wordService) {
        this.botActionHandlers = botActionHandlers;
        this.telegramHelperService = telegramHelperService;
        this.userStateService = userStateService;
        this.wordService = wordService;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        BotAction botAction = getBotAction(update);
        if (botAction != null && EXIT_ACTIONS.contains(botAction)) {
            botActionHandlers.get(botAction).processAction(userState, update);
            return;
        }
        if (TextInput.isStickerOrPhoto(update)) {
            keepAsking(userState, TextInput.MEDIA_REJECTED);
            return;
        }
        if (!takeExample(userState, update)) {
            return;
        }
        WordState wordState = userState.getCurrentWordState();
        if (TextInput.exceeds(wordState.getExampleSentence(), TextInput.EXAMPLE_LIMIT)) {
            keepAsking(userState, TextInput.tooLong(TextInput.EXAMPLE_LIMIT));
            return;
        }
        saveAndReturnToMenu(userState, wordState);
    }

    protected abstract boolean takeExample(UserState userState, Update update);

    protected final void keepAsking(UserState userState, String text) {
        TextInput.keepAsking(userState, telegramHelperService, userStateService, text);
    }

    protected final Optional<String> acceptText(UserState userState, Update update, int limit) {
        return TextInput.accept(userState, telegramHelperService, userStateService, update, limit);
    }

    private void saveAndReturnToMenu(UserState userState, WordState wordState) {
        String text;
        try {
            wordService.addNewWord(wordState, userState.getUserId());
            text = String.format(ADD_WORD_SAVE_WORD.getAnswerMessage(), wordState.getOriginal(),
                    wordState.getTranslation(), wordState.getExampleSentence());
        } catch (DuplicateWordException | DataIntegrityViolationException ex) {
            text = String.format(ADD_WORD_ALREADY_EXISTS.getAnswerMessage(),
                    wordState.getOriginal(), wordState.getTranslation());
        }
        userState.setCurrentWordState(null);
        userState.setProposesState(null);
        userState.setUserStatus(UserStatus.NO_ACTIVITY);
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                sendWithActionButtons(userState.getUserId(), text, BASE_OPTIONS, MENU_BUTTONS_PER_ROW)));
        userStateService.saveUserState(userState);
    }
}
