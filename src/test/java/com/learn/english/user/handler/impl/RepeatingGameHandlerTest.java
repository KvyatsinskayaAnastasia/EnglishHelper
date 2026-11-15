package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.exception.TelegramOperationException;
import com.learn.english.model.BotAction;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import com.learn.english.user.handler.UserStatusHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.learn.english.helper.HandlerTestHelper.MESSAGE_ID;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.assertButtonRows;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.callbackUpdate;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.textUpdate;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.HELP;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_ORIGINAL;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_TRANSLATION;
import static com.learn.english.model.BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RepeatingGameHandlerTest {

    @Mock
    private BotActionHandler chooseTranslationHandler;

    @Mock
    private BotActionHandler chooseOriginalHandler;

    @Mock
    private BotActionHandler writeHandler;

    @Mock
    private BotActionHandler exitHandler;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private UserStateService userStateService;

    private Map<BotAction, BotActionHandler> botActionHandlers;

    @BeforeEach
    void setUp() {
        botActionHandlers = new EnumMap<>(BotAction.class);
        botActionHandlers.put(REPEATING_CHOOSE_TRANSLATION, chooseTranslationHandler);
        botActionHandlers.put(REPEATING_CHOOSE_ORIGINAL, chooseOriginalHandler);
        botActionHandlers.put(REPEATING_WRITE_WORD_BY_TRANSLATION, writeHandler);
        botActionHandlers.put(EXIT, exitHandler);
        stubSend(telegramHelperService);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserStatus.class, names = {"PLANNED_REPEATING_GAME", "RANDOM_REPEATING_GAME"})
    @DisplayName("reports its own repeating status")
    void getUserStatus_returnsOwnStatus(UserStatus status) {
        assertThat(handler(status).getUserStatus()).isEqualTo(status);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserStatus.class, names = {"PLANNED_REPEATING_GAME", "RANDOM_REPEATING_GAME"})
    @DisplayName("starts the translation game and saves the state")
    void processAction_chooseTranslation_delegatesAndSaves(UserStatus status) {
        assertDelegates(status, callbackUpdate(REPEATING_CHOOSE_TRANSLATION.getCode()), chooseTranslationHandler);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserStatus.class, names = {"PLANNED_REPEATING_GAME", "RANDOM_REPEATING_GAME"})
    @DisplayName("starts the original game and saves the state")
    void processAction_chooseOriginal_delegatesAndSaves(UserStatus status) {
        assertDelegates(status, callbackUpdate(REPEATING_CHOOSE_ORIGINAL.getCode()), chooseOriginalHandler);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserStatus.class, names = {"PLANNED_REPEATING_GAME", "RANDOM_REPEATING_GAME"})
    @DisplayName("starts the write-the-word game and saves the state")
    void processAction_writeWord_delegatesAndSaves(UserStatus status) {
        assertDelegates(status, textUpdate(REPEATING_WRITE_WORD_BY_TRANSLATION.getCode()), writeHandler);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserStatus.class, names = {"PLANNED_REPEATING_GAME", "RANDOM_REPEATING_GAME"})
    @DisplayName("routes exit to the exit handler even when a game is already chosen")
    void processAction_exitDuringGame_delegatesToExit(UserStatus status) {
        UserState userState = session(status, REPEATING_CHOOSE_ORIGINAL);
        Update update = callbackUpdate(EXIT.getCode());

        handler(status).processAction(userState, update);

        verify(exitHandler).processAction(userState, update);
        verify(chooseOriginalHandler, never()).processAction(any(), any());
        verify(userStateService).saveUserState(userState);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserStatus.class, names = {"PLANNED_REPEATING_GAME", "RANDOM_REPEATING_GAME"})
    @DisplayName("continues the chosen game when the user sends free text")
    void processAction_freeTextDuringGame_continuesChosenGame(UserStatus status) {
        UserState userState = session(status, REPEATING_WRITE_WORD_BY_TRANSLATION);
        Update update = textUpdate("apple");

        handler(status).processAction(userState, update);

        verify(writeHandler).processAction(userState, update);
        verify(userStateService).saveUserState(userState);
        verify(telegramHelperService, never()).sendMessage(any());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserStatus.class, names = {"PLANNED_REPEATING_GAME", "RANDOM_REPEATING_GAME"})
    @DisplayName("shows the game menu for /help because that command has no action handler")
    void processAction_helpDuringSession_showsGameMenu(UserStatus status) {
        UserState userState = session(status, REPEATING_CHOOSE_TRANSLATION);

        handler(status).processAction(userState, textUpdate(HELP.getCode()));

        assertGameMenu(userState);
        verify(userStateService).saveUserState(userState);
        verify(chooseTranslationHandler, never()).processAction(any(), any());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserStatus.class, names = {"PLANNED_REPEATING_GAME", "RANDOM_REPEATING_GAME"})
    @DisplayName("offers a game again when the message is not a command and a game is not chosen yet")
    void processAction_textBeforeGameChosen_showsGameMenu(UserStatus status) {
        UserState userState = session(status, null);

        handler(status).processAction(userState, textUpdate("hello"));

        assertGameMenu(userState);
        verify(userStateService).saveUserState(userState);
        verifyNoInteractions(chooseTranslationHandler, chooseOriginalHandler, writeHandler, exitHandler);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = UserStatus.class, names = {"PLANNED_REPEATING_GAME", "RANDOM_REPEATING_GAME"})
    @DisplayName("offers a game for an unknown command before a session starts")
    void processAction_unknownWithoutSession_showsGameMenu(UserStatus status) {
        UserState userState = new UserState(USER_ID);

        handler(status).processAction(userState, textUpdate("hello"));

        assertGameMenu(userState);
        verify(userStateService).saveUserState(userState);
        verifyNoInteractions(chooseTranslationHandler, chooseOriginalHandler, writeHandler, exitHandler);
    }

    @Test
    @DisplayName("saves the session when the game handler fails after changing it")
    void processAction_gameHandlerFails_stillSaves() {
        UserState userState = session(UserStatus.PLANNED_REPEATING_GAME, REPEATING_CHOOSE_TRANSLATION);
        Update update = textUpdate("яблоко");
        doThrow(new TelegramOperationException("sendMessage", USER_ID, null, new RuntimeException("down")))
                .when(chooseTranslationHandler).processAction(userState, update);

        assertThatThrownBy(() -> handler(UserStatus.PLANNED_REPEATING_GAME).processAction(userState, update))
                .isInstanceOf(TelegramOperationException.class);

        verify(userStateService).saveUserState(userState);
    }

    private void assertDelegates(UserStatus status, Update update, BotActionHandler expected) {
        UserState userState = new UserState(USER_ID);

        handler(status).processAction(userState, update);

        verify(expected).processAction(userState, update);
        verify(userStateService).saveUserState(userState);
        for (BotActionHandler handler : List.of(chooseTranslationHandler, chooseOriginalHandler, writeHandler, exitHandler)) {
            if (handler != expected) {
                verifyNoInteractions(handler);
            }
        }
    }

    private void assertGameMenu(UserState userState) {
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo(HELP.getAnswerMessage());
        assertThat(callbackData(message)).containsExactly(
                REPEATING_CHOOSE_TRANSLATION.getCode(),
                REPEATING_CHOOSE_ORIGINAL.getCode(),
                REPEATING_WRITE_WORD_BY_TRANSLATION.getCode(),
                EXIT.getCode());
        assertButtonRows(message, 2, 2);
        assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
    }

    private UserStatusHandler handler(UserStatus status) {
        return switch (status) {
            case PLANNED_REPEATING_GAME -> new PlannedRepeatingGameHandler(
                    botActionHandlers, telegramHelperService, userStateService);
            case RANDOM_REPEATING_GAME -> new RandomRepeatingGameHandler(
                    botActionHandlers, telegramHelperService, userStateService);
            default -> throw new IllegalArgumentException("Not a repeating game status: " + status);
        };
    }

    private static UserState session(UserStatus status, BotAction chosenGame) {
        UserState userState = new UserState(USER_ID);
        userState.setUserStatus(status);
        userState.setRepeatingState(new RepeatingState(
                List.of(), null, null, status == UserStatus.PLANNED_REPEATING_GAME, chosenGame));
        return userState;
    }
}
