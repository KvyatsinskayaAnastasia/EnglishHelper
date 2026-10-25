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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.callbackUpdate;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.textUpdate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FillingExampleWithBotHandlerTest {

    @Mock
    private BotActionHandler exitHandler;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private UserStateService userStateService;

    @Mock
    private WordService wordService;

    private FillingExampleWithBotHandler handler;

    @BeforeEach
    void setUp() {
        Map<BotAction, BotActionHandler> botActionHandlers = new EnumMap<>(BotAction.class);
        botActionHandlers.put(EXIT, exitHandler);
        handler = new FillingExampleWithBotHandler(
                botActionHandlers, telegramHelperService, userStateService, wordService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles example selection while proposals are available")
    void getUserStatus_returnsFillingExampleWithBot() {
        assertThat(handler.getUserStatus()).isEqualTo(UserStatus.FILLING_EXAMPLE_WITH_BOT);
    }

    @Test
    @DisplayName("exits without saving the card")
    void processAction_exit_delegates() {
        UserState userState = draft(null, List.of("I eat an apple", "An apple a day"));
        Update update = callbackUpdate(EXIT.getCode());

        handler.processAction(userState, update);

        verify(exitHandler).processAction(userState, update);
        verifyNoInteractions(wordService);
        verify(userStateService, never()).saveUserState(any());
    }

    @Test
    @DisplayName("uses the proposal selected by a numeric callback")
    void processAction_numericCallbackInRange_usesProposal() {
        UserState userState = draft(null, List.of("I eat an apple", "An apple a day"));

        handler.processAction(userState, callbackUpdate("2"));

        assertSaved(userState, "An apple a day");
    }

    @Test
    @DisplayName("uses the proposal selected by a numeric message")
    void processAction_numericMessageInRange_usesProposal() {
        UserState userState = draft("previous", List.of("I eat an apple", "An apple a day"));

        handler.processAction(userState, textUpdate("1"));

        assertSaved(userState, "I eat an apple");
    }

    @Test
    @DisplayName("stores the raw number when it does not match a proposal")
    void processAction_numericOutOfRange_storesRawValue() {
        UserState userState = draft(null, List.of("I eat an apple"));

        handler.processAction(userState, callbackUpdate("5"));

        assertSaved(userState, "5");
    }

    @Test
    @DisplayName("keeps an example already chosen when the number is out of range")
    void processAction_numericOutOfRangeAndExampleSet_keepsExample() {
        UserState userState = draft("already", List.of("I eat an apple"));

        handler.processAction(userState, callbackUpdate("5"));

        assertSaved(userState, "already");
    }

    @Test
    @DisplayName("stores free text when it is not a proposal number")
    void processAction_customText_storesText() {
        UserState userState = draft(null, List.of("I eat an apple"));

        handler.processAction(userState, textUpdate("My own example"));

        assertSaved(userState, "My own example");
    }

    @Test
    @DisplayName("keeps an example already chosen when the user sends free text")
    void processAction_customTextAndExampleSet_keepsExample() {
        UserState userState = draft("already", List.of("I eat an apple"));

        handler.processAction(userState, textUpdate("My own example"));

        assertSaved(userState, "already");
    }

    @Test
    @DisplayName("stores free text when there are no proposals")
    void processAction_noProposals_storesText() {
        UserState userState = draft(null, null);
        userState.setProposesState(null);

        handler.processAction(userState, textUpdate("My own example"));

        assertSaved(userState, "My own example");
    }

    @Test
    @DisplayName("stores free text when the proposal list is empty")
    void processAction_emptyProposals_storesText() {
        UserState userState = draft(null, List.of());

        handler.processAction(userState, textUpdate("My own example"));

        assertSaved(userState, "My own example");
    }

    private void assertSaved(UserState userState, String example) {
        ArgumentCaptor<WordState> captor = ArgumentCaptor.forClass(WordState.class);
        verify(wordService).addNewWord(captor.capture(), eq(USER_ID));
        assertThat(captor.getValue().getOriginal()).isEqualTo("apple");
        assertThat(captor.getValue().getTranslation()).isEqualTo("яблоко");
        assertThat(captor.getValue().getExampleSentence()).isEqualTo(example);
        assertThat(userState.getCurrentWordState()).isNull();
        assertThat(userState.getProposesState()).isNull();
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
        assertThat(onlyMessage(telegramHelperService).getText()).isEqualTo(
                "Добавлено слово apple, в значении яблоко.\nПример использования: " + example + ".\nВыберите действие:");
        verify(userStateService).saveUserState(userState);
    }

    private static UserState draft(String example, List<String> proposals) {
        UserState userState = new UserState(USER_ID);
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE_WITH_BOT);
        userState.setCurrentWordState(new WordState("apple", "яблоко", example));
        ProposesState proposesState = new ProposesState();
        proposesState.setExamples(proposals);
        userState.setProposesState(proposesState);
        return userState;
    }
}
