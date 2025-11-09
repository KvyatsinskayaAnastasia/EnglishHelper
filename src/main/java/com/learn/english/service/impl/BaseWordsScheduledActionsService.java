package com.learn.english.service.impl;

import com.learn.english.service.WordArchiveService;
import com.learn.english.service.WordsScheduledActionsService;
import com.learn.english.service.TelegramService;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseWordsScheduledActionsService implements WordsScheduledActionsService {
    private final WordService wordService;
    private final WordArchiveService wordArchiveService;
    private final TelegramService telegramService;

    @Override
    @Async
    @Scheduled(cron = "0 0 8-23 * * *")
    public void repeatWords() {
        log.info("User notification about words that needs repeating has started");
        wordService.getUsersWithWordsToRepeat().forEach(telegramService::processScheduledRepeat);
    }

    @Override
    @Async
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void moveToArchive() {
        log.info("Learned words moving to the archive repeating has started");
        wordArchiveService.saveArchiveWords(wordService.deleteLearnedWords());
    }
}
