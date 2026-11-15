package com.learn.english.component;

import com.learn.english.configuration.properties.BotProperties;
import com.learn.english.service.TelegramService;
import com.learn.english.utils.TelegramUpdates;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final long UPDATE_TIMEOUT_MINUTES = 2;

    private final TelegramService telegramService;
    private final BotProperties botProperties;
    private final Executor telegramAsyncExecutor;
    private final ConcurrentHashMap<String, UserUpdateQueue> queues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "telegram-lock-cleanup");
        thread.setDaemon(true);
        return thread;
    });

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
        cleanupExecutor.scheduleAtFixedRate(this::cleanupIdleQueues, 1, 1, TimeUnit.MINUTES);
    }

    public void consume(Update update) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        try {
            accept(update, done);
            done.get(UPDATE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("Timed out waiting for update {}", update.getUpdateId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for update {}", update.getUpdateId(), e);
        } catch (ExecutionException e) {
            log.error("Update {} failed while waiting", update.getUpdateId(), e);
        } catch (RuntimeException e) {
            log.error("Failed to accept update {}", update.getUpdateId(), e);
            finish(update, done);
        }
    }

    private void accept(Update update, CompletableFuture<Void> done) {
        String lockKey = lockKey(update);
        if (lockKey == null) {
            submit(() -> finish(update, done));
            return;
        }
        UserUpdateQueue.PendingUpdate pending = new UserUpdateQueue.PendingUpdate(update, done);
        while (true) {
            UserUpdateQueue queue = queues.computeIfAbsent(lockKey, key -> new UserUpdateQueue());
            switch (queue.enqueue(pending)) {
                case START -> {
                    submit(() -> drain(queue));
                    return;
                }
                case QUEUED -> {
                    return;
                }
                case RETIRED -> queues.remove(lockKey, queue);
            }
        }
    }

    private void submit(Runnable task) {
        try {
            telegramAsyncExecutor.execute(task);
        } catch (RejectedExecutionException e) {
            log.error("Async pool rejected a task, running it on the caller thread", e);
            task.run();
        }
    }

    private void drain(UserUpdateQueue queue) {
        UserUpdateQueue.PendingUpdate pending;
        while ((pending = queue.poll()) != null) {
            finish(pending.update(), pending.done());
        }
    }

    private void finish(Update update, CompletableFuture<Void> done) {
        try {
            runSafely(update);
        } finally {
            done.complete(null);
        }
    }

    private void runSafely(Update update) {
        try {
            telegramService.processUpdate(update);
        } catch (Exception e) {
            log.error("Processing failed for update {}", update.getUpdateId(), e);
            notifyUser(update);
        }
    }

    private void notifyUser(Update update) {
        Long userId = TelegramUpdates.userId(update);
        if (userId == null) {
            return;
        }
        try {
            telegramService.notifyProcessingFailed(userId);
        } catch (Exception e) {
            log.error("Failed to tell user {} that processing failed", userId, e);
        }
    }

    private void cleanupIdleQueues() {
        long now = System.currentTimeMillis();
        for (String key : List.copyOf(queues.keySet())) {
            queues.compute(key, (ignored, queue) -> queue != null && queue.retireIfIdle(now) ? null : queue);
        }
    }

    private static String lockKey(Update update) {
        Long userId = TelegramUpdates.userId(update);
        return userId == null ? null : userId.toString();
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
