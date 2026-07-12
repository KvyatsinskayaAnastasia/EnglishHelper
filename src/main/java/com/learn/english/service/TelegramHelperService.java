package com.learn.english.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;


public interface TelegramHelperService {
    Integer sendMessage(SendMessage message);

    void removeKeyboard(Long userId, Integer lastBotCommandMessageId);
}
