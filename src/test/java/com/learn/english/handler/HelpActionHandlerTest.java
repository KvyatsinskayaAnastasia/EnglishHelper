package com.learn.english.handler;

import com.learn.english.handler.impl.HelpActionHandler;
import com.learn.english.model.ProposesState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.learn.english.model.BotAction.HELP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class HelpActionHandlerTest {

    @InjectMocks
    private HelpActionHandler handler;

    @Test
    void botActionSucceedTest() {
        // When
        var result = handler.getBotAction();

        // Then
        assertEquals(HELP, result);
    }

    @Test
    void processActionSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(UserStatus.FILLING_EXAMPLE_WITHOUT_BOT);
        userState.setProposesState(new ProposesState());
        userState.getProposesState().setExamples(List.of("example 1", "example 2"));
        userState.setCurrentWordState(new WordState("translation", "перевод 1", ""));
        
        // When
        var result = handler.processAction(userState, null).get(0);

        // Then
        assertEquals(UserStatus.FILLING_EXAMPLE_WITHOUT_BOT, userState.getUserStatus());
        assertNotNull(userState.getProposesState());
        assertEquals(2, userState.getProposesState().getExamples().size());
        assertNotNull(userState.getCurrentWordState());
        assertNotNull(result);
        assertNotNull(result.getText());
    }
}