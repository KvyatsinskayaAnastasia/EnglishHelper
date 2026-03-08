package com.learn.english.handler;

import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.impl.AddWordWithBotRequestOriginalActionHandler;
import com.learn.english.model.UserState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_ORIGINAL;
import static com.learn.english.model.UserStatus.FILLING_EXAMPLE_WITH_BOT;
import static com.learn.english.model.UserStatus.FILLING_ORIGINAL_WITH_BOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AddWordWithBotRequestOriginalActionHandlerTest {

    @InjectMocks
    private AddWordWithBotRequestOriginalActionHandler handler;

    @Test
    void botActionSucceedTest() {
        // When
        var action = handler.getBotAction();

        // Then
        assertEquals(ADD_WORD_WITH_BOT_REQUEST_ORIGINAL, action);
    }

    @Test
    void processActionSucceedTest() {
        // Given
        var userState = new UserState(123L);

        // When
        var result = handler.processAction(userState, null).get(0);

        // Then
        assertNotNull(result);
        assertEquals(ADD_WORD_WITH_BOT_REQUEST_ORIGINAL.getAnswerMessage(), result.getText());
        assertEquals(String.valueOf(userState.getUserId()), result.getChatId());
        assertNotNull(result.getReplyMarkup());
        assertNotNull(userState.getCurrentWordState());
        assertEquals(FILLING_ORIGINAL_WITH_BOT, userState.getUserStatus());
    }

    @Test
    void processActionFailedWithIncorrectUserStatusTest() {
        // Given
        var incorrectUserStatus = FILLING_EXAMPLE_WITH_BOT;
        var userState = new UserState(123L);
        userState.setUserStatus(incorrectUserStatus);

        // When & Then
        assertThrows(BadUserStatusException.class, () -> handler.processAction(userState, null));
        assertEquals(incorrectUserStatus, userState.getUserStatus());
    }
}