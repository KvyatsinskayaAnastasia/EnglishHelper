package com.learn.english.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface TelegramService {

    void processUpdate(Update update);

    void processScheduledRepeat(long userId);
}
