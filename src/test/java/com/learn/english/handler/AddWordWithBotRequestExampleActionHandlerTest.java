package com.learn.english.handler;

import com.learn.english.exception.BadActionSentMessageException;
import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.impl.AddWordWithBotRequestExampleActionHandler;
import com.learn.english.model.ProposesState;
import com.learn.english.model.UserState;
import com.learn.english.model.WordState;
import com.learn.english.service.OllamaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_EXAMPLE;
import static com.learn.english.model.UserStatus.FILLING_EXAMPLE_WITH_BOT;
import static com.learn.english.model.UserStatus.FILLING_TRANSLATION_WITH_BOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddWordWithBotRequestExampleActionHandlerTest {

    @InjectMocks
    private AddWordWithBotRequestExampleActionHandler handler;

    @Mock
    private OllamaService ollamaService;

    @Test
    void botActionSucceedTest() {
        // When
        var action = handler.getBotAction();

        // Then
        assertEquals(ADD_WORD_WITH_BOT_REQUEST_EXAMPLE, action);
    }

    @Test
    void processActionWithTextMessageSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_TRANSLATION_WITH_BOT);
        var original = "translation";
        var translation = "перевод";
        userState.setCurrentWordState(new WordState(original, null, null));
        userState.setProposesState(new ProposesState());

        // When
        when(ollamaService.proposeExamples(anyString(), anyString())).thenReturn(List.of("example 1", "example 2"));
        var result = handler.processAction(userState, translation).get(0);

        // Then
        verify(ollamaService).proposeExamples(original, translation);
        assertNotNull(result);
        assertTrue(result.getText().contains(String.format(ADD_WORD_WITH_BOT_REQUEST_EXAMPLE.getAnswerMessage(),
                userState.getCurrentWordState().getOriginal(),
                userState.getCurrentWordState().getTranslation())));
        assertEquals(String.valueOf(userState.getUserId()), result.getChatId());
        assertNotNull(result.getReplyMarkup());
        assertEquals(FILLING_EXAMPLE_WITH_BOT, userState.getUserStatus());
        assertEquals(translation, userState.getCurrentWordState().getTranslation());
    }

    @Test
    void processActionWithNumericMessageSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_TRANSLATION_WITH_BOT);
        var original = "translation";
        userState.setCurrentWordState(new WordState(original, null, null));
        String selectedTranslation = "перевод 2";
        ProposesState proposesState = new ProposesState();
        proposesState.setTranslations(Arrays.asList("перевод 1", selectedTranslation, "перевод 3"));
        userState.setProposesState(proposesState);

        // When
        when(ollamaService.proposeExamples(anyString(), anyString())).thenReturn(List.of("example 1", "example 2"));
        var result = handler.processAction(userState, "2").get(0);

        // Then
        verify(ollamaService).proposeExamples(original, selectedTranslation);
        assertNotNull(result);
        assertTrue(result.getText().contains(String.format(ADD_WORD_WITH_BOT_REQUEST_EXAMPLE.getAnswerMessage(),
                userState.getCurrentWordState().getOriginal(),
                userState.getCurrentWordState().getTranslation())));
        assertEquals(String.valueOf(userState.getUserId()), result.getChatId());
        assertNotNull(result.getReplyMarkup());
        assertEquals(FILLING_EXAMPLE_WITH_BOT, userState.getUserStatus());
        assertEquals(selectedTranslation, userState.getCurrentWordState().getTranslation());
    }

    @Test
    void processActionFailedWithEmptyTranslationTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_TRANSLATION_WITH_BOT);
        userState.setCurrentWordState(new WordState("translation", null, null));

        // When & Then
        assertThrows(BadActionSentMessageException.class, () -> handler.processAction(userState, ""));
        assertNull(userState.getCurrentWordState().getTranslation());
    }

    @Test
    void processActionFailedWithIncorrectUserStatusTest() {
        // Given
        var incorrectUserStatus = FILLING_EXAMPLE_WITH_BOT;
        var userState = new UserState(123L);
        userState.setUserStatus(incorrectUserStatus);
        userState.setCurrentWordState(new WordState("translation", null, null));

        // When & Then
        assertThrows(BadUserStatusException.class, () -> handler.processAction(userState, "перевод"));
        assertEquals(incorrectUserStatus, userState.getUserStatus());
    }
}