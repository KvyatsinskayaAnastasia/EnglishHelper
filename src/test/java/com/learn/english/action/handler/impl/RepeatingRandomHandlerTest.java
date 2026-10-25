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

import java.util.List;
import java.util.UUID;

import static com.learn.english.model.BotAction.ADD_WORD_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_ORIGINAL;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_TRANSLATION;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.model.BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.emptyUpdate;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.word;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepeatingRandomHandlerTest {

    private static final UUID WORD_ID = UUID.randomUUID();

    @Mock
    private UserStateService userStateService;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private WordService wordService;

    private RepeatingRandomHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RepeatingRandomHandler(userStateService, telegramHelperService, wordService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles random repeating")
    void getBotAction_returnsRepeatingRandom() {
        assertThat(handler.getBotAction()).isEqualTo(REPEATING_RANDOM);
    }

    @Test
    @DisplayName("asks the user to add a word when the dictionary is empty")
    void processAction_noWords_asksToAdd() {
        when(wordService.getRandomWordEOSByUserId(USER_ID, 10)).thenReturn(List.of());
        UserState userState = new UserState(USER_ID);
        userState.setUserStatus(UserStatus.NO_ACTIVITY);

        handler.processAction(userState, emptyUpdate());

        assertThat(userState.getRepeatingState()).isNull();
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo("Что бы что-то повторить, нужно что-то добавить");
        assertThat(callbackData(message)).containsExactly(ADD_WORD_REQUEST_ORIGINAL.getCode());
        verify(userStateService).saveUserState(userState);
    }

    @Test
    @DisplayName("loads up to ten random words without increasing the repeating counter flag")
    void processAction_wordsFound_startsSession() {
        WordForRepeat card = word(WORD_ID, "apple", "яблоко");
        when(wordService.getRandomWordEOSByUserId(USER_ID, 10)).thenReturn(List.of(card));
        UserState userState = new UserState(USER_ID);

        handler.processAction(userState, emptyUpdate());

        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.RANDOM_REPEATING_GAME);
        RepeatingState repeatingState = userState.getRepeatingState();
        assertThat(repeatingState.getRepeatingWords()).containsExactly(card);
        assertThat(repeatingState.isIncreaseRepeatingCount()).isFalse();
        assertThat(repeatingState.getChosenRepeatingGame()).isNull();
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo("Давай повторять вот эти слова:\n\n"
                + String.format("%s: %s.%nПример: %s", card.getOriginal(), card.getTranslation(), card.getExampleSentence()));
        assertThat(callbackData(message)).containsExactly(
                REPEATING_CHOOSE_TRANSLATION.getCode(),
                REPEATING_CHOOSE_ORIGINAL.getCode(),
                REPEATING_WRITE_WORD_BY_TRANSLATION.getCode(),
                EXIT.getCode());
        verify(userStateService).saveUserState(userState);
    }
}
