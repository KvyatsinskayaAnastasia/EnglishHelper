package com.learn.english.handler;

import com.learn.english.exception.BadActionSentMessageException;
import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.impl.AddWordWithBotRequestTranslationActionHandler;
import com.learn.english.model.ProposesState;
import com.learn.english.model.UserState;
import com.learn.english.model.WordState;
import com.learn.english.service.OllamaService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.learn.english.model.BotAction.ADD_WORD_WITH_BOT_REQUEST_TRANSLATION;
import static com.learn.english.model.UserStatus.FILLING_EXAMPLE_WITH_BOT;
import static com.learn.english.model.UserStatus.FILLING_ORIGINAL_WITH_BOT;
import static com.learn.english.model.UserStatus.FILLING_TRANSLATION_WITH_BOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddWordWithBotRequestTranslationActionHandlerTest {

    @InjectMocks
    private AddWordWithBotRequestTranslationActionHandler handler;

    @Mock
    private OllamaService ollamaService;

    @Test
    void botActionSucceedTest() {
        // When
        var action = handler.getBotAction();

        // Then
        assertEquals(ADD_WORD_WITH_BOT_REQUEST_TRANSLATION, action);
    }

    @Test
    void processActionSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_ORIGINAL_WITH_BOT);
        userState.setCurrentWordState(new WordState());
        userState.setProposesState(new ProposesState());
        var original = "translation";

        // When
        when(ollamaService.proposeTranslations(anyString())).thenReturn(List.of("перевод 1", "перевод 2"));
        var result = handler.processAction(userState, original).get(0);

        // Then
        verify(ollamaService).proposeTranslations(original);
        assertNotNull(result);
        assertTrue(result.getText().contains(String.format(ADD_WORD_WITH_BOT_REQUEST_TRANSLATION.getAnswerMessage(),
                userState.getCurrentWordState().getOriginal())));
        assertEquals(String.valueOf(userState.getUserId()), result.getChatId());
        assertNotNull(result.getReplyMarkup());
        assertEquals(FILLING_TRANSLATION_WITH_BOT, userState.getUserStatus());
        assertEquals(original, userState.getCurrentWordState().getOriginal());
    }

    @Test
    void processActionFailedWithBlankOriginalTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_ORIGINAL_WITH_BOT);
        userState.setCurrentWordState(new WordState("", null, null));

        // When & Then
        assertThrows(BadActionSentMessageException.class, () -> handler.processAction(userState, ""));
        assertTrue(StringUtils.isBlank(userState.getCurrentWordState().getOriginal()));
    }

    @Test
    void processActionFailedWithIncorrectUserStatusTest() {
        // Given
        var incorrectUserStatus = FILLING_EXAMPLE_WITH_BOT;
        var userState = new UserState(123L);
        userState.setUserStatus(incorrectUserStatus);
        userState.setCurrentWordState(new WordState());

        // When & Then
        assertThrows(BadUserStatusException.class, () -> handler.processAction(userState, "translation"));
        assertEquals(incorrectUserStatus, userState.getUserStatus());
    }
}