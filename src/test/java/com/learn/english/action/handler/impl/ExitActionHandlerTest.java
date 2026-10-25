package com.learn.english.action.handler.impl;

import com.learn.english.model.BotAction;
import com.learn.english.model.ProposesState;
import com.learn.english.model.RepeatingState;
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

import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.helper.HandlerTestHelper.MESSAGE_ID;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.assertButtonRows;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.emptyUpdate;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExitActionHandlerTest {

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private UserStateService userStateService;

    private ExitActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ExitActionHandler(telegramHelperService, userStateService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles the exit command")
    void getBotAction_returnsExit() {
        assertThat(handler.getBotAction()).isEqualTo(EXIT);
    }

    @Test
    @DisplayName("clears the draft, proposals and repeating session and shows the main menu")
    void processAction_resetsStateAndShowsMenu() {
        UserState userState = new UserState(USER_ID);
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState("test", "тест", "I need to test this bot!"));
        userState.setProposesState(new ProposesState());
        userState.setRepeatingState(new RepeatingState(List.of(), null, null, true, REPEATING_PLANNED));

        handler.processAction(userState, emptyUpdate());

        assertThat(userState.getCurrentWordState()).isNull();
        assertThat(userState.getProposesState()).isNull();
        assertThat(userState.getRepeatingState()).isNull();
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
        assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo(BotAction.EXIT.getAnswerMessage());
        assertThat(callbackData(message)).containsExactly(
                ADD_WORD_REQUEST_ORIGINAL.getCode(),
                REPEATING_PLANNED.getCode(),
                REPEATING_RANDOM.getCode());
        assertButtonRows(message, 2, 1);
        verify(userStateService).saveUserState(userState);
    }
}
