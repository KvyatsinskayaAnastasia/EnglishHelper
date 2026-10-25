package com.learn.english.user.handler.impl;

import com.learn.english.action.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordState;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.UserStateService;
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

import static com.learn.english.model.BotAction.EXIT;
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

@ExtendWith(MockitoExtension.class)
class FillingOriginalWithoutBotHandlerTest {

    @Mock
    private BotActionHandler exitHandler;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private UserStateService userStateService;

    private FillingOriginalWithoutBotHandler handler;

    @BeforeEach
    void setUp() {
        Map<BotAction, BotActionHandler> botActionHandlers = new EnumMap<>(BotAction.class);
        botActionHandlers.put(EXIT, exitHandler);
        handler = new FillingOriginalWithoutBotHandler(botActionHandlers, telegramHelperService, userStateService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles manual entry of the original word")
    void getUserStatus_returnsFillingOriginalWithoutBot() {
        assertThat(handler.getUserStatus()).isEqualTo(UserStatus.FILLING_ORIGINAL_WITHOUT_BOT);
    }

    @Test
    @DisplayName("leaves the draft untouched and exits")
    void processAction_exit_delegates() {
        UserState userState = draft();
        Update update = callbackUpdate(EXIT.getCode());

        handler.processAction(userState, update);

        verify(exitHandler).processAction(userState, update);
        assertThat(userState.getCurrentWordState().getOriginal()).isNull();
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.FILLING_ORIGINAL_WITHOUT_BOT);
        verify(telegramHelperService, never()).sendMessage(any());
        verify(userStateService, never()).saveUserState(any());
    }

    @Test
    @DisplayName("stores the original and asks for a translation")
    void processAction_text_movesToTranslation() {
        UserState userState = draft();

        handler.processAction(userState, textUpdate("apple"));

        assertThat(userState.getCurrentWordState().getOriginal()).isEqualTo("apple");
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.FILLING_TRANSLATION_WITHOUT_BOT);
        assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo("Добавление нового слова.\nВведите перевод слова apple");
        assertThat(callbackData(message)).containsExactly(EXIT.getCode());
        assertButtonRows(message, 1);
        verify(userStateService).saveUserState(userState);
        verify(exitHandler, never()).processAction(any(), any());
    }

    private static UserState draft() {
        UserState userState = new UserState(USER_ID);
        userState.setUserStatus(UserStatus.FILLING_ORIGINAL_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState());
        return userState;
    }
}
