package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.ProposesState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordState;
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
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.EnumMap;
import java.util.Map;

import static com.learn.english.model.BotAction.ADD_WORD_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.helper.HandlerTestHelper.MESSAGE_ID;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.assertButtonRows;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.callbackUpdate;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.textUpdate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FillingExampleWithoutBotHandlerTest {

    @Mock
    private BotActionHandler exitHandler;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private UserStateService userStateService;

    @Mock
    private WordService wordService;

    private FillingExampleWithoutBotHandler handler;

    @BeforeEach
    void setUp() {
        Map<BotAction, BotActionHandler> botActionHandlers = new EnumMap<>(BotAction.class);
        botActionHandlers.put(EXIT, exitHandler);
        handler = new FillingExampleWithoutBotHandler(
                botActionHandlers, telegramHelperService, userStateService, wordService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles manual entry of the example")
    void getUserStatus_returnsFillingExampleWithoutBot() {
        assertThat(handler.getUserStatus()).isEqualTo(UserStatus.FILLING_EXAMPLE_WITHOUT_BOT);
    }

    @Test
    @DisplayName("exits without saving the card")
    void processAction_exit_delegates() {
        UserState userState = draft(null);
        Update update = callbackUpdate(EXIT.getCode());

        handler.processAction(userState, update);

        verify(exitHandler).processAction(userState, update);
        verifyNoInteractions(wordService);
        verify(telegramHelperService, never()).sendMessage(any());
        verify(userStateService, never()).saveUserState(any());
    }

    @Test
    @DisplayName("saves the card with the entered example and returns to the menu")
    void processAction_blankExample_savesEnteredText() {
        UserState userState = draft(null);
        WordState draft = userState.getCurrentWordState();

        handler.processAction(userState, textUpdate("I eat an apple"));

        assertThat(draft.getExampleSentence()).isEqualTo("I eat an apple");
        verify(wordService).addNewWord(draft, USER_ID);
        assertSavedAndBackToMenu(userState, "I eat an apple");
    }

    @Test
    @DisplayName("keeps an example that was already filled in")
    void processAction_exampleAlreadySet_keepsIt() {
        UserState userState = draft("already saved");
        WordState draft = userState.getCurrentWordState();

        handler.processAction(userState, textUpdate("a different example"));

        assertThat(draft.getExampleSentence()).isEqualTo("already saved");
        verify(wordService).addNewWord(draft, USER_ID);
        assertSavedAndBackToMenu(userState, "already saved");
    }

    private void assertSavedAndBackToMenu(UserState userState, String example) {
        assertThat(userState.getCurrentWordState()).isNull();
        assertThat(userState.getProposesState()).isNull();
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
        assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo(
                "Добавлено слово apple, в значении яблоко.\nПример использования: " + example + ".\nВыберите действие:");
        assertThat(callbackData(message)).containsExactly(
                ADD_WORD_REQUEST_ORIGINAL.getCode(),
                REPEATING_PLANNED.getCode(),
                REPEATING_RANDOM.getCode());
        assertButtonRows(message, 2, 1);
        verify(userStateService).saveUserState(userState);
    }

    private static UserState draft(String example) {
        UserState userState = new UserState(USER_ID);
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState("apple", "яблоко", example));
        userState.setProposesState(new ProposesState());
        return userState;
    }
}
