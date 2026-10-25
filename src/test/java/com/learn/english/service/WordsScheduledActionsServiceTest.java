package com.learn.english.service;

import com.learn.english.entity.WordEO;
import com.learn.english.service.impl.BaseWordsScheduledActionsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordsScheduledActionsServiceTest {

    @Mock
    private WordService wordService;

    @Mock
    private WordArchiveService wordArchiveService;

    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private BaseWordsScheduledActionsService service;

    @Nested
    @DisplayName("repeatWords")
    class RepeatWordsTests {

        @Test
        @DisplayName("notifies every user who has words due for review")
        void repeatWords_notifiesEachUser() {
            when(wordService.getUsersWithWordsToRepeat()).thenReturn(List.of(1L, 2L));

            service.repeatWords();

            verify(telegramService).processScheduledRepeat(1L);
            verify(telegramService).processScheduledRepeat(2L);
        }

        @Test
        @DisplayName("does not call Telegram when nobody has words to repeat")
        void repeatWords_noUsers() {
            when(wordService.getUsersWithWordsToRepeat()).thenReturn(Collections.emptyList());

            service.repeatWords();

            verifyNoInteractions(telegramService);
        }
    }

    @Nested
    @DisplayName("moveToArchive")
    class MoveToArchiveTests {

        @Test
        @DisplayName("archives the list returned by deleteLearnedWords")
        void moveToArchive_savesDeletedWords() {
            WordEO learned = new WordEO();
            List<WordEO> learnedWords = List.of(learned);
            when(wordService.deleteLearnedWords()).thenReturn(learnedWords);

            service.moveToArchive();

            InOrder order = inOrder(wordService, wordArchiveService);
            order.verify(wordService).deleteLearnedWords();
            order.verify(wordArchiveService).saveArchiveWords(learnedWords);
        }
    }
}
