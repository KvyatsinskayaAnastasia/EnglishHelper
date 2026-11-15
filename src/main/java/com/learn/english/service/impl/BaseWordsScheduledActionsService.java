package com.learn.english.service.impl;

import com.learn.english.service.WordsScheduledActionsService;
import com.learn.english.service.TelegramService;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseWordsScheduledActionsService implements WordsScheduledActionsService {
    static final String SCHEDULE_ZONE = "${bot.schedule.zone:Europe/Moscow}";
    static final String REPEAT_CRON = "0 0 8-23 * * *";
    static final String ARCHIVE_CRON = "0 0 0 * * *";

    private final WordService wordService;
    private final TelegramService telegramService;

    @Override
    @Scheduled(cron = REPEAT_CRON, zone = SCHEDULE_ZONE)
    public void repeatWords() {
        log.info("User notification about words that needs repeating has started");
        for (Long userId : wordService.getUsersWithWordsToRepeat()) {
            try {
                telegramService.processScheduledRepeat(userId);
            } catch (RuntimeException e) {
                log.error("Failed to notify user {} about words to repeat", userId, e);
            }
        }
    }

    @Override
    @Scheduled(cron = ARCHIVE_CRON, zone = SCHEDULE_ZONE)
    public void moveToArchive() {
        log.info("Learned words moving to the archive repeating has started");
        wordService.moveLearnedWordsToArchive();
    }
}
