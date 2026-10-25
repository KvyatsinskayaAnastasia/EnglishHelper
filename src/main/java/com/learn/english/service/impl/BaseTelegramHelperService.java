package com.learn.english.service.impl;

import com.learn.english.exception.TelegramOperationException;
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

    private final TelegramClient telegramClient;

    @Override
    public Integer sendMessage(SendMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Couldn't send null message");
        }

        try {
            Integer messageId =  telegramClient.execute(message).getMessageId();
            log.debug("Message sent: chatId={}, messageId={}", message.getChatId(), messageId);
            return messageId;
        } catch (TelegramApiException e) {
            log.error("Failed to send message: chatId={}", message.getChatId(), e);
            throw new TelegramOperationException("sendMessage", message.getChatId(), null, e);
        }
    }

    @Override
    public void removeKeyboard(Long chatId, Integer lastBotCommandMessageId) {
        if (chatId == null || lastBotCommandMessageId == null) {
            log.warn("Couldn't remove inline keyboard: chatId={}, messageId={}", chatId,
                    lastBotCommandMessageId);
            return;
        }

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(chatId);
        editMarkup.setMessageId(lastBotCommandMessageId);
        editMarkup.setReplyMarkup(null);

        try {
            telegramClient.execute(editMarkup);
            log.debug("Inline keyboard removed: chatId={}, messageId={}", chatId, lastBotCommandMessageId);
        } catch (TelegramApiException e) {
            log.error("Failed to remove inline keyboard: chatId={}, messageId={}", chatId, lastBotCommandMessageId, e);
            throw new TelegramOperationException("removeKeyboard", chatId, lastBotCommandMessageId, e);
        }
    }
}