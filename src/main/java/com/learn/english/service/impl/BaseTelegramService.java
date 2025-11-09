package com.learn.english.service.impl;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramService;
import com.learn.english.service.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;

import static com.learn.english.model.BotAction.SCHEDULED_REPEAT;
import static com.learn.english.model.BotAction.getOptionFromValue;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseTelegramService implements TelegramService {

    private static final List<BotAction> LONG_PROCESSING_ACTIONS = List.of(
            BotAction.ADD_WORD_WITH_BOT_REQUEST_TRANSLATION,
            BotAction.ADD_WORD_WITH_BOT_REQUEST_EXAMPLE,
            BotAction.REGENERATE_EXAMPLES,
            BotAction.REGENERATE_TRANSLATIONS
    );

    private static final List<BotAction> ALLOWED_CALLBACK_ACTIONS = List.of(
            BotAction.EXIT,
            BotAction.REGENERATE_EXAMPLES,
            BotAction.REGENERATE_TRANSLATIONS
    );
    private static final String BAD_ACTION = "Что-то не то нажато =.=";

    private final Map<UserStatus, BotAction> nextBotActions = Map.of(
            UserStatus.FILLING_WORD, BotAction.ADD_WORD_WITH_BOT_REQUEST_TRANSLATION,
            UserStatus.FILLING_TRANSLATION, BotAction.ADD_WORD_WITH_BOT_REQUEST_EXAMPLE,
            UserStatus.FILLING_EXAMPLE, BotAction.ADD_WORD_SAVE_WORD,
            UserStatus.FILLING_WORD_WITHOUT_BOT, BotAction.ADD_WORD_REQUEST_TRANSLATION,
            UserStatus.FILLING_TRANSLATION_WITHOUT_BOT, BotAction.ADD_WORD_REQUEST_EXAMPLE,
            UserStatus.FILLING_EXAMPLE_WITHOUT_BOT, BotAction.ADD_WORD_SAVE_WORD,
            UserStatus.REPEATING_WRITE_WORD_BY_TRANSLATION, BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION,
            UserStatus.REPEATING_CHOSE_TRANSLATION, BotAction.REPEATING_CHOSE_TRANSLATION,
            UserStatus.REPEATING_CHOOSE_ORIGINAL, BotAction.REPEATING_CHOOSE_ORIGINAL
    );

    private static final String WAITING_MESSAGE = "Бот жив, но долго думает. Терпение...";

    private final Map<BotAction, BotActionHandler> botActionHandlers;
    private final TelegramClient telegramClient;
    private final UserStateService userStateService;

    @Override
    public void processScheduledRepeat(long userId) {
        UserState userState = userStateService.getUserState(userId);
        SendMessage sendMessage = botActionHandlers.get(SCHEDULED_REPEAT)
                .processAction(userState, null);
        sendMessageIfNotNull(sendMessage);
        userStateService.saveUserState(userState);
    }

    @Override
    public void processUpdate(Update update) {
        processMessage(update, update.getCallbackQuery() != null);
    }

    private void processMessage(Update update, boolean isCallback) {
        Long userId = getUserId(update, isCallback);
        UserState userState = userStateService.getUserState(userId);
        if (userState.getLastBotCommandMessageId() != null) {
            log.info("Clean bot command buttons");
            cleanBotMessage(userState);
        }

        String text = getText(update, isCallback);
        BotAction botAction = nextBotActions.get(userState.getUserStatus());
        if (botAction != null && requiresWaitingMessage(botAction)) {
            log.info("Send waiting message for command: {}", botAction.getMessage());
            sendWaitingMessage(userId);
        }

        SendMessage sendMessage = botActionHandlers.getOrDefault(
                        isCallback ?
                                determineCallbackAction(botAction, text, userId)
                                : botAction,
                        botActionHandlers.get(BotAction.HELP))
                .processAction(userState, !isCallback || botAction != null ? text : null);
        var messageId = sendMessageIfNotNull(sendMessage);
        if (sendMessage.getReplyMarkup() != null) {
            if (userState.getLastBotCommandMessageId() != null) {
                log.info("Added new bot command buttons. Clean bot command buttons");
                cleanBotMessage(userState);
            }
            log.info("Added new bot command buttons. Save message id");
            userState.setLastBotCommandMessageId(messageId);
        }
        userStateService.saveUserState(userState);
    }

    private Long getUserId(Update update, boolean isCallback) {
        return isCallback ?
                update.getCallbackQuery().getFrom().getId() :
                update.getMessage().getFrom().getId();
    }

    private String getText(Update update, boolean isCallback) {
        return isCallback ?
                update.getCallbackQuery().getData() :
                update.getMessage().getText();
    }

    private boolean requiresWaitingMessage(BotAction action) {
        return LONG_PROCESSING_ACTIONS.contains(action);
    }

    private BotAction determineCallbackAction(BotAction currentAction, String callbackData, Long userId) {
        BotAction userAction = getOptionFromValue(callbackData);

        if (currentAction != null && userAction != null &&
                !ALLOWED_CALLBACK_ACTIONS.contains(userAction)) {
            sendMessage(SendMessage.builder().chatId(userId).text(BAD_ACTION).build());
            return null;
        }

        return userAction != null ? userAction : currentAction;
    }

    private void sendWaitingMessage(Long userId) {
        sendMessage(SendMessage.builder().chatId(userId).text(WAITING_MESSAGE).build());
    }

    private Integer sendMessageIfNotNull(SendMessage message) {
        if (message == null) {
            return null;
        }
        return sendMessage(message);
    }

    private void cleanBotMessage(UserState userState) {
        try {
            EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
            editMarkup.setChatId(userState.getUserId());
            editMarkup.setMessageId(userState.getLastBotCommandMessageId());
            editMarkup.setReplyMarkup(null);
            telegramClient.execute(editMarkup);
            userState.setLastBotCommandMessageId(null);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Telegram API error", e);
        }
    }

    private Integer sendMessage(SendMessage message) {
        try {
           return telegramClient.execute(message).getMessageId();
        } catch (TelegramApiException e) {
            throw new RuntimeException("Telegram API error", e);
        }
    }
}