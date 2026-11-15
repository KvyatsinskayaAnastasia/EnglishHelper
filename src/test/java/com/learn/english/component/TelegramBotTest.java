package com.learn.english.component;

import com.learn.english.configuration.properties.BotProperties;
import com.learn.english.service.TelegramService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelegramBotTest {

    @Mock
    private TelegramService telegramService;

    @Mock
    private BotProperties botProperties;

    private ExecutorService pool;
    private TelegramBot bot;

    @AfterEach
    void tearDown() {
        if (bot != null) {
            bot.shutdown();
        }
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("updates of one user run one at a time and none are dropped")
    void consume_sameUser_runsSequentially() throws Exception {
        bot = newBot();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        doAnswer(invocation -> {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            inFlight.decrementAndGet();
            return null;
        }).when(telegramService).processUpdate(any());

        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = callers.submit(() -> bot.consume(userMessage(1)));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> second = callers.submit(() -> bot.consume(userMessage(1)));
            Thread.sleep(200);
            assertThat(maxInFlight.get()).isEqualTo(1);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            callers.shutdownNow();
        }

        verify(telegramService, times(2)).processUpdate(any());
    }

    @Test
    @DisplayName("updates of different users can run at the same time")
    void consume_differentUsers_runConcurrently() throws Exception {
        bot = newBot();
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            return null;
        }).when(telegramService).processUpdate(any());

        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = callers.submit(() -> bot.consume(userMessage(1)));
            Future<?> second = callers.submit(() -> bot.consume(userMessage(2)));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            callers.shutdownNow();
        }
    }

    @Test
    @DisplayName("tells the user when processing throws and still accepts the update")
    void consume_processingFails_notifiesUser() {
        bot = newBot();
        doThrow(new IllegalStateException("boom")).when(telegramService).processUpdate(any());

        bot.consume(userMessage(5));

        verify(telegramService).notifyProcessingFailed(5L);
    }

    @Test
    @DisplayName("an update without a user does not fail the polling thread")
    void consume_withoutUser_doesNotNotify() {
        bot = newBot();

        bot.consume(new Update());

        verify(telegramService).processUpdate(any());
        verify(telegramService, never()).notifyProcessingFailed(any(Long.class));
    }

    private TelegramBot newBot() {
        pool = Executors.newFixedThreadPool(4);
        return new TelegramBot(telegramService, botProperties, pool);
    }

    private static Update userMessage(long userId) {
        Update update = new Update();
        update.setUpdateId((int) userId);
        Message message = new Message();
        message.setFrom(new User(userId, "Ann", false));
        update.setMessage(message);
        return update;
    }
}
