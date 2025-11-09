package com.learn.english.component;

import com.learn.english.configuration.properties.BotProperties;
import com.learn.english.service.TelegramService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramService telegramService;
    private final BotProperties botProperties;
    private final Executor telegramAsyncExecutor;
    private final ConcurrentHashMap<String, LockInfo> locks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);

    public TelegramBot(TelegramService telegramService, BotProperties botProperties,
                       @Qualifier("telegramAsyncExecutor") Executor telegramAsyncExecutor) {
        this.telegramService = telegramService;
        this.botProperties = botProperties;
        this.telegramAsyncExecutor = telegramAsyncExecutor;
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @PostConstruct
    public void init() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleLocks, 1, 1, TimeUnit.MINUTES);
    }

    public void consume(Update update) {
        String lockKey = generateLockKey(update);

        CompletableFuture.runAsync(() -> {
            if (lockKey != null && !acquireLock(lockKey)) {
                log.debug("Blocked concurrent processing for key: {}", lockKey);
                return;
            }

            try {
                telegramService.processUpdate(update);
            } catch (Exception e) {
                log.error("Processing failed for update {}: {}", update.getUpdateId(), e.getMessage());
            } finally {
                if (lockKey != null) {
                    releaseLock(lockKey);
                }
            }
        }, telegramAsyncExecutor);
    }

    private boolean acquireLock(String lockKey) {
        LockInfo lockInfo = locks.compute(lockKey, (key, existing) -> {
            if (existing == null || !existing.isLocked()) {
                return new LockInfo(new ReentrantLock(), System.currentTimeMillis());
            }
            return existing;
        });

        return lockInfo.getLock().tryLock();
    }

    private void releaseLock(String lockKey) {
        LockInfo lockInfo = locks.get(lockKey);
        if (lockInfo != null) {
            lockInfo.getLock().unlock();
        }
    }

    private void cleanupStaleLocks() {
        long now = System.currentTimeMillis();
        locks.entrySet().removeIf(entry -> {
            LockInfo lockInfo = entry.getValue();
            return !lockInfo.isLocked() && (now - lockInfo.getTimestamp() > 300000);
        });
    }

    private String generateLockKey(Update update) {
        Long userId = null;

        if (update.hasMessage()) {
            userId = update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()) {
            userId = update.getCallbackQuery().getFrom().getId();
        }

        return (userId != null) ? userId.toString() : null;
    }

    @Getter
    @AllArgsConstructor
    private static class LockInfo {
        private final ReentrantLock lock;
        private final long timestamp;

        public boolean isLocked() {
            return lock.isLocked();
        }
    }

    @PreDestroy
    public void shutdown() {
        cleanupExecutor.shutdown();
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        log.info("Bot registered successfully.");
        if (!botSession.isRunning()) {
            log.warn("Bot session is not running!");
        }
    }
}
