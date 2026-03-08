package com.learn.english.handler;

import com.learn.english.exception.BadActionSentMessageException;
import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.impl.AddWordWithoutBotRequestExampleActionHandler;
import com.learn.english.model.UserState;
import com.learn.english.model.WordState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.learn.english.model.BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_EXAMPLE;
import static com.learn.english.model.UserStatus.FILLING_EXAMPLE_WITHOUT_BOT;
import static com.learn.english.model.UserStatus.FILLING_TRANSLATION_WITHOUT_BOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AddWordWithoutBotRequestExampleActionHandlerTest {

    @InjectMocks
    private AddWordWithoutBotRequestExampleActionHandler handler;

    @Test
    void botActionSucceedTest() {
        // When
        var action = handler.getBotAction();

        // Then
        assertEquals(ADD_WORD_WITHOUT_BOT_REQUEST_EXAMPLE, action);
    }

    @Test
    void processActionSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_TRANSLATION_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState("translation", null, null));
        var translation = "перевод";

        // When
        var result = handler.processAction(userState, translation).get(0);

        // Then
        assertNotNull(result);
        assertTrue(result.getText().contains(String.format(ADD_WORD_WITHOUT_BOT_REQUEST_EXAMPLE.getAnswerMessage(),
                userState.getCurrentWordState().getOriginal(),
                userState.getCurrentWordState().getTranslation())));
        assertEquals(String.valueOf(userState.getUserId()), result.getChatId());
        assertNotNull(result.getReplyMarkup());
        assertEquals(FILLING_EXAMPLE_WITHOUT_BOT, userState.getUserStatus());
        assertEquals(translation, userState.getCurrentWordState().getTranslation());
    }

    @Test
    void processActionFailedWithEmptyTranslationTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_TRANSLATION_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState("translation", null, null));

        // When & Then
        assertThrows(BadActionSentMessageException.class, () -> handler.processAction(userState, ""));
        assertNull(userState.getCurrentWordState().getTranslation());
    }

    @Test
    void processActionFailedWithIncorrectUserStatusTest() {
        // Given
        var incorrectUserStatus = FILLING_EXAMPLE_WITHOUT_BOT;
        var userState = new UserState(123L);
        userState.setUserStatus(incorrectUserStatus);
        userState.setCurrentWordState(new WordState("translation", null, null));

        // When & Then
        assertThrows(BadUserStatusException.class, () -> handler.processAction(userState, "перевод"));
        assertEquals(incorrectUserStatus, userState.getUserStatus());
    }
}