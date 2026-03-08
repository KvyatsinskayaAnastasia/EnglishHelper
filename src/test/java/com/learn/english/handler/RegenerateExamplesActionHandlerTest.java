package com.learn.english.handler;

import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.impl.RegenerateExamplesActionHandler;
import com.learn.english.model.ProposesState;
import com.learn.english.model.UserState;
import com.learn.english.model.WordState;
import com.learn.english.service.OllamaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.learn.english.model.BotAction.REGENERATE_EXAMPLES;
import static com.learn.english.model.UserStatus.FILLING_EXAMPLE_WITHOUT_BOT;
import static com.learn.english.model.UserStatus.FILLING_EXAMPLE_WITH_BOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegenerateExamplesActionHandlerTest {

    @InjectMocks
    private RegenerateExamplesActionHandler handler;

    @Mock
    private OllamaService ollamaService;

    @Test
    void botActionSucceedTest() {
        // When
        var action = handler.getBotAction();

        // Then
        assertEquals(REGENERATE_EXAMPLES, action);
    }

    @Test
    void processActionSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_EXAMPLE_WITH_BOT);
        userState.setProposesState(new ProposesState());
        userState.setCurrentWordState(new WordState("translation", "перевод", null));

        // When
        when(ollamaService.proposeExamples(anyString(), anyString())).thenReturn(List.of("example 1", "example 2"));
        var result = handler.processAction(userState, null).get(0);

        // Then
        assertNotNull(result);
        assertTrue(result.getText().contains(String.format(REGENERATE_EXAMPLES.getAnswerMessage(),
                userState.getCurrentWordState().getOriginal(), userState.getCurrentWordState().getTranslation())));
        assertEquals(String.valueOf(userState.getUserId()), result.getChatId());
        assertNotNull(result.getReplyMarkup());
        assertEquals(FILLING_EXAMPLE_WITH_BOT, userState.getUserStatus());
        assertEquals(2, userState.getProposesState().getExamples().size());
        verify(ollamaService).proposeExamples(userState.getCurrentWordState().getOriginal(),
                userState.getCurrentWordState().getTranslation());
    }

    @Test
    void processActionFailedWithIncorrectUserStatusTest() {
        // Given
        var incorrectUserStatus = FILLING_EXAMPLE_WITHOUT_BOT;
        var userState = new UserState(123L);
        userState.setUserStatus(incorrectUserStatus);

        // When & Then
        assertThrows(BadUserStatusException.class, () -> handler.processAction(userState, "translation"));
        assertEquals(incorrectUserStatus, userState.getUserStatus());
        verify(ollamaService, times(0)).proposeTranslations(anyString());
    }
}