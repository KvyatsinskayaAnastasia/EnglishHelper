package com.learn.english.action.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.DailyRepeatCount;
import com.learn.english.model.UserState;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordArchiveService;
import com.learn.english.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.learn.english.model.BotAction.HELP;
import static com.learn.english.model.BotAction.SUMMARY;
import static com.learn.english.utils.ButtonUtils.sendWithActionButtons;

@Component
@RequiredArgsConstructor
public class SummaryActionHandler implements BotActionHandler {
    private static final int WEEK_DAYS = 7;
    private static final int MENU_BUTTONS_PER_ROW = 2;
    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, EEE", Locale.forLanguageTag("ru"));
    private static final String MESSAGE_TEMPLATE = """
            Слов на изучении: %d

            План повторений на неделю:
            %s

            Изучено слов: %d

            %s""";

    private final UserStateService userStateService;
    private final TelegramHelperService telegramHelperService;
    private final WordService wordService;
    private final WordArchiveService wordArchiveService;

    @Override
    public BotAction getBotAction() {
        return SUMMARY;
    }

    @Override
    public void processAction(UserState userState, Update update) {
        Long userId = userState.getUserId();
        String text = formatMessage(
                wordService.countLearningWords(userId),
                wordService.countRepeatsForNextDays(userId, WEEK_DAYS),
                wordArchiveService.countArchivedWords(userId));
        userState.setLastBotCommandMessageId(telegramHelperService.sendMessage(
                sendWithActionButtons(userId, text, BASE_OPTIONS, MENU_BUTTONS_PER_ROW)));
        userStateService.saveUserState(userState);
    }

    private static String formatMessage(int learningWords, List<DailyRepeatCount> plan, int learnedWords) {
        LocalDate today = LocalDate.now();
        String days = plan.stream()
                .map(day -> formatDay(day, today))
                .collect(Collectors.joining("\n"));
        return MESSAGE_TEMPLATE.formatted(learningWords, days, learnedWords, HELP.getAnswerMessage());
    }

    private static String formatDay(DailyRepeatCount day, LocalDate today) {
        String todayMark = day.date().equals(today) ? " (сегодня)" : "";
        return DAY_FORMAT.format(day.date()) + todayMark + ": " + day.count();
    }
}
