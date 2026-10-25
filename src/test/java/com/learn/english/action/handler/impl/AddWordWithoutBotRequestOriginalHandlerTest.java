package com.learn.english.action.handler.impl;

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

import static com.learn.english.model.BotAction.ADD_WORD_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.helper.HandlerTestHelper.MESSAGE_ID;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.emptyUpdate;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddWordWithoutBotRequestOriginalHandlerTest {

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private UserStateService userStateService;

    private AddWordWithoutBotRequestOriginalHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AddWordWithoutBotRequestOriginalHandler(telegramHelperService, userStateService);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles the add-word command")
    void getBotAction_returnsAddWordRequestOriginal() {
        assertThat(handler.getBotAction()).isEqualTo(ADD_WORD_REQUEST_ORIGINAL);
    }

    @Test
    @DisplayName("opens a fresh draft and asks for the original word")
    void processAction_startsManualWordEntry() {
        UserState userState = new UserState(USER_ID);
        WordState previous = new WordState("test", "тест", "I need to test this bot!");
        userState.setCurrentWordState(previous);

        handler.processAction(userState, emptyUpdate());

        assertThat(userState.getCurrentWordState()).isNotSameAs(previous);
        assertThat(userState.getCurrentWordState().getOriginal()).isNull();
        assertThat(userState.getCurrentWordState().getTranslation()).isNull();
        assertThat(userState.getCurrentWordState().getExampleSentence()).isNull();
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.FILLING_ORIGINAL_WITHOUT_BOT);
        assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo("Добавление нового слова.\nВведите слово:");
        assertThat(callbackData(message)).containsExactly(EXIT.getCode());
        verify(userStateService).saveUserState(userState);
    }
}
