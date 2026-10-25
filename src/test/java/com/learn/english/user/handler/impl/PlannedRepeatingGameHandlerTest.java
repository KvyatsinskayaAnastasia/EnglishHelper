package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.HELP;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_ORIGINAL;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_TRANSLATION;
import static com.learn.english.model.BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.callbackUpdate;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.textUpdate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlannedRepeatingGameHandlerTest {

    @Mock
    private BotActionHandler chooseTranslationHandler;

    @Mock
    private BotActionHandler chooseOriginalHandler;

    @Mock
    private BotActionHandler writeHandler;

    @Mock
    private BotActionHandler exitHandler;

    @Mock
    private BotActionHandler helpHandler;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private UserStateService userStateService;

    private PlannedRepeatingGameHandler handler;

    @BeforeEach
    void setUp() {
        Map<BotAction, BotActionHandler> botActionHandlers = new EnumMap<>(BotAction.class);
        botActionHandlers.put(REPEATING_CHOOSE_TRANSLATION, chooseTranslationHandler);
        botActionHandlers.put(REPEATING_CHOOSE_ORIGINAL, chooseOriginalHandler);
        botActionHandlers.put(REPEATING_WRITE_WORD_BY_TRANSLATION, writeHandler);
        botActionHandlers.put(EXIT, exitHandler);
        botActionHandlers.put(HELP, helpHandler);
        handler = new PlannedRepeatingGameHandler(botActionHandlers, telegramHelperService, userStateService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles choosing a planned repeating game")
    void getUserStatus_returnsPlannedRepeatingGame() {
        assertThat(handler.getUserStatus()).isEqualTo(UserStatus.PLANNED_REPEATING_GAME);
    }

    @Nested
    @DisplayName("processAction")
    class ProcessActionTests {

        @Test
        @DisplayName("starts the chosen game and saves the state")
        void processAction_chooseTranslation_delegatesAndSaves() {
            UserState userState = new UserState(USER_ID);
            Update update = callbackUpdate(REPEATING_CHOOSE_TRANSLATION.getCode());

            handler.processAction(userState, update);

            verify(chooseTranslationHandler).processAction(userState, update);
            verify(userStateService).saveUserState(userState);
            verifyNoInteractions(chooseOriginalHandler, writeHandler, exitHandler);
        }

        @Test
        @DisplayName("routes exit to the exit handler even when a game is already chosen")
        void processAction_exitDuringGame_delegatesToExit() {
            UserState userState = session(REPEATING_CHOOSE_ORIGINAL);
            Update update = callbackUpdate(EXIT.getCode());

            handler.processAction(userState, update);

            verify(exitHandler).processAction(userState, update);
            verify(chooseOriginalHandler, never()).processAction(any(), any());
            verify(userStateService).saveUserState(userState);
        }

        @Test
        @DisplayName("continues the chosen game when the user sends free text")
        void processAction_freeTextDuringGame_continuesChosenGame() {
            UserState userState = session(REPEATING_WRITE_WORD_BY_TRANSLATION);
            Update update = textUpdate("apple");

            handler.processAction(userState, update);

            verify(writeHandler).processAction(userState, update);
            verify(userStateService).saveUserState(userState);
            verify(telegramHelperService, never()).sendMessage(any());
        }

        @Test
        @DisplayName("sends another recognized command to its own handler while a game is open")
        void processAction_otherCommandDuringGame_usesThatCommand() {
            UserState userState = session(REPEATING_CHOOSE_TRANSLATION);
            Update update = textUpdate(HELP.getCode());

            handler.processAction(userState, update);

            verify(helpHandler).processAction(userState, update);
            verify(chooseTranslationHandler, never()).processAction(any(), any());
            verify(userStateService).saveUserState(userState);
        }

        @Test
        @DisplayName("fails to offer a game for an unknown command because the action has no prompt text")
        void processAction_unknownWithoutSession_throws() {
            UserState userState = new UserState(USER_ID);

            assertThatThrownBy(() -> handler.processAction(userState, textUpdate("hello")))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("text is marked non-null but is null");

            verify(userStateService, never()).saveUserState(any());
            verify(telegramHelperService, never()).sendMessage(any());
            verifyNoInteractions(chooseTranslationHandler, chooseOriginalHandler, writeHandler, exitHandler, helpHandler);
        }
    }

    private static UserState session(BotAction chosenGame) {
        UserState userState = new UserState(USER_ID);
        userState.setUserStatus(UserStatus.PLANNED_REPEATING_GAME);
        userState.setRepeatingState(new RepeatingState(List.of(), null, null, true, chosenGame));
        return userState;
    }
}
