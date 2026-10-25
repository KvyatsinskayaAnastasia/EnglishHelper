package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.EnumMap;
import java.util.Map;

import static com.learn.english.model.BotAction.ADD_WORD_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.HELP;
import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.helper.HandlerTestHelper.MESSAGE_ID;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.assertButtonRows;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.callbackUpdate;
import static com.learn.english.helper.HandlerTestHelper.emptyUpdate;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.textUpdate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NoActivityHandlerTest {

    @Mock
    private BotActionHandler addWordHandler;

    @Mock
    private BotActionHandler plannedHandler;

    @Mock
    private BotActionHandler randomHandler;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private UserStateService userStateService;

    private NoActivityHandler handler;

    @BeforeEach
    void setUp() {
        Map<BotAction, BotActionHandler> botActionHandlers = new EnumMap<>(BotAction.class);
        botActionHandlers.put(ADD_WORD_REQUEST_ORIGINAL, addWordHandler);
        botActionHandlers.put(REPEATING_PLANNED, plannedHandler);
        botActionHandlers.put(REPEATING_RANDOM, randomHandler);
        handler = new NoActivityHandler(botActionHandlers, telegramHelperService, userStateService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles the idle status")
    void getUserStatus_returnsNoActivity() {
        assertThat(handler.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
    }

    @Nested
    @DisplayName("processAction")
    class ProcessActionTests {

        @Test
        @DisplayName("starts adding a word when that command arrives")
        void processAction_addWord_delegates() {
            UserState userState = new UserState(USER_ID);
            Update update = callbackUpdate(ADD_WORD_REQUEST_ORIGINAL.getCode());

            handler.processAction(userState, update);

            verify(addWordHandler).processAction(userState, update);
            verify(userStateService, never()).saveUserState(any());
            verify(telegramHelperService, never()).sendMessage(any());
        }

        @Test
        @DisplayName("starts a planned repeating session from a text command")
        void processAction_plannedRepeating_delegates() {
            UserState userState = new UserState(USER_ID);
            Update update = textUpdate(REPEATING_PLANNED.getCode());

            handler.processAction(userState, update);

            verify(plannedHandler).processAction(userState, update);
            verifyNoInteractions(addWordHandler, randomHandler);
        }

        @Test
        @DisplayName("starts a random repeating session from a text command")
        void processAction_randomRepeating_delegates() {
            UserState userState = new UserState(USER_ID);
            Update update = textUpdate(REPEATING_RANDOM.getCode());

            handler.processAction(userState, update);

            verify(randomHandler).processAction(userState, update);
        }

        @Test
        @DisplayName("shows the main menu for arbitrary text")
        void processAction_unknownText_showsMenu() {
            UserState userState = new UserState(USER_ID);

            handler.processAction(userState, textUpdate("hello"));

            SendMessage message = onlyMessage(telegramHelperService);
            assertThat(message.getText()).isEqualTo(HELP.getAnswerMessage());
            assertThat(callbackData(message)).containsExactly(
                    ADD_WORD_REQUEST_ORIGINAL.getCode(),
                    REPEATING_PLANNED.getCode(),
                    REPEATING_RANDOM.getCode());
            assertButtonRows(message, 2, 1);
            assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
            verify(userStateService).saveUserState(userState);
            verifyNoInteractions(addWordHandler, plannedHandler, randomHandler);
        }

        @Test
        @DisplayName("shows the main menu when the update has no message and no callback")
        void processAction_emptyUpdate_showsMenu() {
            UserState userState = new UserState(USER_ID);

            handler.processAction(userState, emptyUpdate());

            assertThat(onlyMessage(telegramHelperService).getText()).isEqualTo(HELP.getAnswerMessage());
            verify(userStateService).saveUserState(userState);
        }

        @Test
        @DisplayName("shows the main menu for a command that is not available while idle")
        void processAction_exitWhileIdle_showsMenu() {
            UserState userState = new UserState(USER_ID);

            handler.processAction(userState, textUpdate(BotAction.EXIT.getCode()));

            assertThat(onlyMessage(telegramHelperService).getText()).isEqualTo("Выберите действие:");
            verify(userStateService).saveUserState(userState);
        }
    }
}
