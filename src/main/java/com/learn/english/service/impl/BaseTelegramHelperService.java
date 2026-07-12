package com.learn.english.service.impl;

import com.learn.english.service.TelegramHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseTelegramHelperService implements TelegramHelperService {

    private static final String BAD_ACTION = "Что-то не то нажато =.=";
    private static final String WAITING_MESSAGE = "Бот жив, но долго думает. Терпение...";

    private final TelegramClient telegramClient;

    private void sendWaitingMessage(Long userId) {
        sendMessage(SendMessage.builder().chatId(userId).text(WAITING_MESSAGE).build());
    }

    @Override
    public void removeKeyboard(Long userId, Integer lastBotCommandMessageId) {
        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(userId);
        editMarkup.setMessageId(lastBotCommandMessageId);
        editMarkup.setReplyMarkup(null);
        try {
            telegramClient.execute(editMarkup);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Telegram API error", e);
        }
    }

    @Override
    public Integer sendMessage(SendMessage message) {
        try {
            return telegramClient.execute(message).getMessageId();
        } catch (TelegramApiException e) {
            throw new RuntimeException("Telegram API error", e);
        }
    }
}