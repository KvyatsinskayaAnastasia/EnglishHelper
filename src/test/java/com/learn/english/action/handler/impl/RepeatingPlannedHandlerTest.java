package com.learn.english.action.handler.impl;

import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.learn.english.model.BotAction.ADD_WORD_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_ORIGINAL;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_TRANSLATION;
import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.model.BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;
import static com.learn.english.helper.HandlerTestHelper.MESSAGE_ID;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.assertButtonRows;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.emptyUpdate;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.word;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepeatingPlannedHandlerTest {

    private static final UUID FIRST_ID = UUID.randomUUID();
    private static final UUID SECOND_ID = UUID.randomUUID();

    @Mock
    private UserStateService userStateService;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private WordService wordService;

    private RepeatingPlannedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RepeatingPlannedHandler(userStateService, telegramHelperService, wordService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles planned repeating")
    void getBotAction_returnsRepeatingPlanned() {
        assertThat(handler.getBotAction()).isEqualTo(REPEATING_PLANNED);
    }

    @Test
    @DisplayName("loads up to ten due words and offers a game")
    void processAction_wordsDue_startsSession() {
        WordForRepeat first = word(FIRST_ID, "apple", "яблоко");
        WordForRepeat second = word(SECOND_ID, "dog", "собака");
        List<WordForRepeat> words = new ArrayList<>(List.of(first, second));
        when(wordService.getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(USER_ID, 10)).thenReturn(words);
        UserState userState = new UserState(USER_ID);

        handler.processAction(userState, emptyUpdate());

        words.clear();
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.PLANNED_REPEATING_GAME);
        RepeatingState repeatingState = userState.getRepeatingState();
        assertThat(repeatingState.getRepeatingWords()).containsExactly(first, second);
        assertThat(repeatingState.getCurrentRepeatingWord()).isNull();
        assertThat(repeatingState.getRepeatingLabels()).isNull();
        assertThat(repeatingState.isIncreaseRepeatingCount()).isTrue();
        assertThat(repeatingState.getChosenRepeatingGame()).isNull();
        assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo("""
                Давай повторять вот эти слова:\s
                
                apple: яблоко.
                Пример: example of apple
                
                dog: собака.
                Пример: example of dog""");
        assertThat(callbackData(message)).containsExactly(
                REPEATING_CHOOSE_TRANSLATION.getCode(),
                REPEATING_CHOOSE_ORIGINAL.getCode(),
                REPEATING_WRITE_WORD_BY_TRANSLATION.getCode(),
                EXIT.getCode());
        assertButtonRows(message, 1, 1, 1, 1);
        verify(userStateService).saveUserState(userState);
    }

    @Test
    @DisplayName("reports that everything is repeated and still switches to the planned game status")
    void processAction_nothingDue_reportsCompletion() {
        when(wordService.getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(USER_ID, 10)).thenReturn(List.of());
        UserState userState = new UserState(USER_ID);
        RepeatingState previous = new RepeatingState();
        userState.setRepeatingState(previous);

        handler.processAction(userState, emptyUpdate());

        assertThat(userState.getRepeatingState()).isSameAs(previous);
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.PLANNED_REPEATING_GAME);
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo("Все слова повторены!");
        assertThat(callbackData(message)).containsExactly(
                ADD_WORD_REQUEST_ORIGINAL.getCode(),
                REPEATING_PLANNED.getCode(),
                REPEATING_RANDOM.getCode());
        assertButtonRows(message, 2, 1);
        verify(userStateService).saveUserState(userState);
    }
}
