package com.learn.english.handler;

import com.learn.english.exception.BadActionSentMessageException;
import com.learn.english.exception.BadUserStatusException;
import com.learn.english.handler.impl.AddWordSaveWordActionHandler;
import com.learn.english.model.ProposesState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordState;
import com.learn.english.service.WordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static com.learn.english.model.BotAction.ADD_WORD_SAVE_WORD;
import static com.learn.english.model.UserStatus.FILLING_EXAMPLE_WITHOUT_BOT;
import static com.learn.english.model.UserStatus.FILLING_TRANSLATION_WITHOUT_BOT;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddWordSaveWordActionHandlerTest {

    @Mock
    private WordService wordService;

    @InjectMocks
    private AddWordSaveWordActionHandler handler;

    @Test
    void botActionSucceedTest() {
        // When
        var result = handler.getBotAction();

        // Then
        assertEquals(ADD_WORD_SAVE_WORD, result);
    }

    @Test
    void processActionWithTextMessageSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_EXAMPLE_WITHOUT_BOT);
        var wordState = new WordState("translation", "перевод", null);
        userState.setCurrentWordState(wordState);

        // When
        var result = handler.processAction(userState, "example").get(0);

        // Then
        verify(wordService).addNewWord(wordState, userState.getUserId());
        assertNull(userState.getCurrentWordState());
        assertNull(userState.getProposesState());
        assertEquals(UserStatus.NO_ACTIVITY, userState.getUserStatus());
        assertNotNull(result);
        assertTrue(result.getText().contains(String.format(ADD_WORD_SAVE_WORD.getAnswerMessage(),
                wordState.getOriginal(),
                wordState.getTranslation(),
                wordState.getExampleSentence())));
    }

    @Test
    void processActionWithNumericMessageSucceedTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_EXAMPLE_WITHOUT_BOT);
        var wordState = new WordState("translation", "перевод", null);
        userState.setCurrentWordState(wordState);
        String selectedExample = "Example 2";
        ProposesState proposesState = new ProposesState();
        proposesState.setExamples(Arrays.asList("Example 1", selectedExample, "Example 3"));
        userState.setProposesState(proposesState);

        // When
        var result = handler.processAction(userState, "2").get(0);

        // Then
        verify(wordService).addNewWord(wordState, userState.getUserId());
        assertNull(userState.getCurrentWordState());
        assertNull(userState.getProposesState());
        assertEquals(UserStatus.NO_ACTIVITY, userState.getUserStatus());
        assertNotNull(result);
    }

    @Test
    void processActionFailedWithIncorrectUserStatusTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_TRANSLATION_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState("translation", "перевод", null));

        // When & Then
        assertThrows(BadUserStatusException.class,
                () -> handler.processAction(userState, "example"));
        assertEquals(UserStatus.FILLING_TRANSLATION_WITHOUT_BOT, userState.getUserStatus());
    }

    @Test
    void processActionFailedWithEmptyMessageTest() {
        // Given
        var userState = new UserState(123L);
        userState.setUserStatus(FILLING_EXAMPLE_WITHOUT_BOT);
        userState.setCurrentWordState(new WordState("translation", "перевод", null));

        // When & Then
        assertThrows(BadActionSentMessageException.class, () -> handler.processAction(userState, " "));
    }
}