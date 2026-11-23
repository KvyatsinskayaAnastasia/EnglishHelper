package com.learn.english.action.handler.impl;

import com.learn.english.model.DailyRepeatCount;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordArchiveService;
import com.learn.english.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static com.learn.english.model.BotAction.HELP;
import static com.learn.english.model.BotAction.SUMMARY;
import static com.learn.english.helper.HandlerTestHelper.MESSAGE_ID;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.assertButtonRows;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.emptyUpdate;
import static com.learn.english.helper.HandlerTestHelper.mainMenuCodes;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryActionHandlerTest {

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, EEE", Locale.forLanguageTag("ru"));
    private static final int[] PLAN_COUNTS = {2, 0, 1, 0, 0, 4, 0};

    @Mock
    private UserStateService userStateService;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private WordService wordService;

    @Mock
    private WordArchiveService wordArchiveService;

    private SummaryActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SummaryActionHandler(
                userStateService, telegramHelperService, wordService, wordArchiveService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles the summary command")
    void getBotAction_returnsSummary() {
        assertThat(handler.getBotAction()).isEqualTo(SUMMARY);
        assertThat(SUMMARY.getButtonMessage()).isEqualTo("Сводка");
    }

    @Test
    @DisplayName("shows learning words, the next seven days and the archive total")
    void processAction_sendsCountsAndWeeklyPlan() {
        LocalDate today = LocalDate.now();
        List<DailyRepeatCount> plan = IntStream.range(0, PLAN_COUNTS.length)
                .mapToObj(i -> new DailyRepeatCount(today.plusDays(i), PLAN_COUNTS[i]))
                .toList();
        when(wordService.countLearningWords(USER_ID)).thenReturn(12);
        when(wordService.countRepeatsForNextDays(USER_ID, 7)).thenReturn(plan);
        when(wordArchiveService.countArchivedWords(USER_ID)).thenReturn(30);
        UserState userState = new UserState(USER_ID);

        handler.processAction(userState, emptyUpdate());

        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
        assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo(expectedMessage(today));
        assertThat(callbackData(message)).containsExactly(mainMenuCodes());
        assertButtonRows(message, 2, 2);
        verify(userStateService).saveUserState(userState);
    }

    private static String expectedMessage(LocalDate today) {
        String days = IntStream.range(0, PLAN_COUNTS.length)
                .mapToObj(i -> dayLine(today.plusDays(i), PLAN_COUNTS[i], i == 0))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return """
                Слов на изучении: 12

                План повторений на неделю:
                %s

                Изучено слов: 30

                %s""".formatted(days, HELP.getAnswerMessage());
    }

    private static String dayLine(LocalDate date, int count, boolean today) {
        String todayMark = today ? " (сегодня)" : "";
        return DAY_FORMAT.format(date) + todayMark + ": " + count;
    }
}
