package com.learn.english.handler;

import com.learn.english.exception.BadActionSentMessageException;
import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.impl.AddWordWithoutBotRequestTranslationActionHandler;
import com.learn.english.model.UserState;
import com.learn.english.model.WordState;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.learn.english.model.BotAction.ADD_WORD_WITHOUT_BOT_REQUEST_TRANSLATION;
import static com.learn.english.model.UserStatus.FILLING_EXAMPLE_WITHOUT_BOT;
import static com.learn.english.model.UserStatus.FILLING_TRANSLATION_WITHOUT_BOT;
import static com.learn.english.model.UserStatus.FILLING_ORIGINAL_WITHOUT_BOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AddWordWithoutBotRequestTranslationActionHandlerTest {

    @InjectMocks
    private AddWordWithoutBotRequestTranslationActionHandler handler;

    @Test
    void botActionSucceedTest() {
        // When
        var action = handler.getBotAction();

        // Then
        assertEquals(ADD_WORD_WITHOUT_BOT_REQUEST_TRANSLATION, action);
    }

    @Test
    void processActionSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_ORIGINAL_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState());
        var original = "translation";

        // When
        var result = handler.processAction(userState, original).get(0);

        // Then
        assertNotNull(result);
        assertTrue(result.getText().contains(String.format(ADD_WORD_WITHOUT_BOT_REQUEST_TRANSLATION.getAnswerMessage(),
                userState.getCurrentWordState().getOriginal())));
        assertEquals(String.valueOf(userState.getUserId()), result.getChatId());
        assertNotNull(result.getReplyMarkup());
        assertEquals(FILLING_TRANSLATION_WITHOUT_BOT, userState.getUserStatus());
        assertEquals(original, userState.getCurrentWordState().getOriginal());
    }

    @Test
    void processActionFailedWithNullCurrentWordStateTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_ORIGINAL_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState());

        // When & Then
        assertThrows(BadActionSentMessageException.class, () -> handler.processAction(userState, ""));
        assertNull(userState.getCurrentWordState().getOriginal());
    }

    @Test
    void processActionFailedWithBlankOriginalTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_ORIGINAL_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState("", null, null));

        // When & Then
        assertThrows(BadActionSentMessageException.class, () -> handler.processAction(userState, ""));
        assertTrue(StringUtils.isBlank(userState.getCurrentWordState().getOriginal()));
    }

    @Test
    void processActionFailedWithIncorrectUserStatusTest() {
        // Given
        var incorrectUserStatus = FILLING_EXAMPLE_WITHOUT_BOT;
        var userState = new UserState(123L);
        userState.setUserStatus(incorrectUserStatus);
        userState.setCurrentWordState(new WordState());

        // When & Then
        assertThrows(BadUserStatusException.class, () -> handler.processAction(userState, "translation"));
        assertEquals(incorrectUserStatus, userState.getUserStatus());
    }
}