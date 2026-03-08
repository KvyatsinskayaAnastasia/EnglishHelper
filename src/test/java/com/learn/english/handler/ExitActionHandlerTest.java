package com.learn.english.handler;

import com.learn.english.handler.impl.ExitActionHandler;
import com.learn.english.model.ProposesState;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.learn.english.model.BotAction.EXIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class ExitActionHandlerTest {

    @InjectMocks
    private ExitActionHandler handler;

    @Test
    void botActionSucceedTest() {
        // When
        var result = handler.getBotAction();

        // Then
        assertEquals(EXIT, result);
    }

    @Test
    void processActionSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState("word", "слово", null));
        userState.setProposesState(new ProposesState());
        userState.setRepeatingState(new RepeatingState());

        // When
        var result = handler.processAction(userState, null).get(0);

        // Then
        assertNull(userState.getCurrentWordState());
        assertNull(userState.getProposesState());
        assertNull(userState.getRepeatingState());
        assertEquals(UserStatus.NO_ACTIVITY, userState.getUserStatus());
        assertNotNull(result);
        assertNotNull(result.getText());
    }
}