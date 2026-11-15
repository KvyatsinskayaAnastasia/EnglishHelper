package com.learn.english.service;

import com.learn.english.exception.TelegramOperationException;
import com.learn.english.service.impl.BaseWordsScheduledActionsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordsScheduledActionsServiceTest {

    @Mock
    private WordService wordService;

    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private BaseWordsScheduledActionsService service;

    @Nested
    @DisplayName("repeatWords")
    class RepeatWordsTests {

        @Test
        @DisplayName("notifies every user who has words due for review")
        void repeatWords_notifiesEachUser() {
            when(wordService.getUsersWithWordsToRepeat()).thenReturn(List.of(1L, 2L));

            service.repeatWords();

            verify(telegramService).processScheduledRepeat(1L);
            verify(telegramService).processScheduledRepeat(2L);
        }

        @Test
        @DisplayName("notifies the remaining users when Telegram fails for one of them")
        void repeatWords_continuesAfterFailure() {
            when(wordService.getUsersWithWordsToRepeat()).thenReturn(List.of(1L, 2L, 3L));
            doNothing().when(telegramService).processScheduledRepeat(1L);
            doThrow(new TelegramOperationException("sendMessage", 2L, null, new RuntimeException("down")))
                    .when(telegramService).processScheduledRepeat(2L);
            doNothing().when(telegramService).processScheduledRepeat(3L);

            service.repeatWords();

            var order = inOrder(telegramService);
            order.verify(telegramService).processScheduledRepeat(1L);
            order.verify(telegramService).processScheduledRepeat(2L);
            order.verify(telegramService).processScheduledRepeat(3L);
        }

        @Test
        @DisplayName("sends reminders from 08:00 through 23:00 in the schedule zone, not UTC")
        void repeatWords_runsInScheduleZone() throws NoSuchMethodException {
            Scheduled scheduled = BaseWordsScheduledActionsService.class
                    .getMethod("repeatWords")
                    .getAnnotation(Scheduled.class);
            assertThat(scheduled.cron()).isEqualTo("0 0 8-23 * * *");
            assertThat(scheduled.zone()).isEqualTo("${bot.schedule.zone:Europe/Moscow}");
            assertThat(BaseWordsScheduledActionsService.class.getMethod("repeatWords").getAnnotation(Async.class))
                    .isNull();

            CronExpression expression = CronExpression.parse(scheduled.cron());
            ZoneId zone = ZoneId.of("Europe/Moscow");

            assertThat(expression.next(ZonedDateTime.of(2026, 11, 15, 7, 30, 0, 0, zone)))
                    .isEqualTo(ZonedDateTime.of(2026, 11, 15, 8, 0, 0, 0, zone));
            assertThat(expression.next(ZonedDateTime.of(2026, 11, 15, 22, 30, 0, 0, zone)))
                    .isEqualTo(ZonedDateTime.of(2026, 11, 15, 23, 0, 0, 0, zone));
            assertThat(expression.next(ZonedDateTime.of(2026, 11, 15, 23, 30, 0, 0, zone)))
                    .isEqualTo(ZonedDateTime.of(2026, 11, 16, 8, 0, 0, 0, zone));
        }

        @Test
        @DisplayName("does not call Telegram when nobody has words to repeat")
        void repeatWords_noUsers() {
            when(wordService.getUsersWithWordsToRepeat()).thenReturn(Collections.emptyList());

            service.repeatWords();

            verifyNoInteractions(telegramService);
        }
    }

    @Nested
    @DisplayName("moveToArchive")
    class MoveToArchiveTests {

        @Test
        @DisplayName("delegates archiving to the word service transaction")
        void moveToArchive_delegatesToWordService() {
            service.moveToArchive();

            verify(wordService).moveLearnedWordsToArchive();
        }

        @Test
        @DisplayName("archives learned words at local midnight in the schedule zone")
        void moveToArchive_runsAtLocalMidnight() throws NoSuchMethodException {
            Scheduled scheduled = BaseWordsScheduledActionsService.class
                    .getMethod("moveToArchive")
                    .getAnnotation(Scheduled.class);
            assertThat(scheduled.cron()).isEqualTo("0 0 0 * * *");
            assertThat(scheduled.zone()).isEqualTo("${bot.schedule.zone:Europe/Moscow}");

            CronExpression expression = CronExpression.parse(scheduled.cron());
            ZoneId zone = ZoneId.of("Europe/Moscow");
            assertThat(expression.next(ZonedDateTime.of(2026, 11, 15, 23, 30, 0, 0, zone)))
                    .isEqualTo(ZonedDateTime.of(2026, 11, 16, 0, 0, 0, 0, zone));
        }
    }
}
