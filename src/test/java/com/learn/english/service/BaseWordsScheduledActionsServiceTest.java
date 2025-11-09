package com.learn.english.service;

import com.learn.english.service.impl.BaseWordsScheduledActionsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseWordsScheduledActionsServiceTest {

    @Mock
    private WordService wordService;

    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private BaseWordsScheduledActionsService repeatWordsService;

    @Captor
    private ArgumentCaptor<Long> userIdCaptor;

    @Test
    void repeatWords_ShouldProcessAllUsersWithWordsToRepeat() {
        // Arrange
        Long userId1 = 456L;
        Long userId2 = 101L;
        List<Long> usersWithWords = List.of(userId1, userId2);

        when(wordService.getUsersWithWordsToRepeat()).thenReturn(usersWithWords);
        doNothing().when(telegramService).processScheduledRepeat(anyLong());

        // Act
        repeatWordsService.repeatWords();

        // Assert
        verify(wordService, times(1)).getUsersWithWordsToRepeat();
        verify(telegramService, times(1)).processScheduledRepeat(456L);
        verify(telegramService, times(1)).processScheduledRepeat(101L);
        verifyNoMoreInteractions(telegramService);
    }

    @Test
    void repeatWords_ShouldNotProcessAnything_WhenNoUsersWithWords() {
        // Arrange
        when(wordService.getUsersWithWordsToRepeat()).thenReturn(List.of());

        // Act
        repeatWordsService.repeatWords();

        // Assert
        verify(wordService, times(1)).getUsersWithWordsToRepeat();
        verify(telegramService, never()).processScheduledRepeat(anyLong());
    }

    @Test
    void repeatWords_ShouldHandleEmptyList() {
        // Arrange
        when(wordService.getUsersWithWordsToRepeat()).thenReturn(List.of());

        // Act
        repeatWordsService.repeatWords();

        // Assert
        verify(wordService, times(1)).getUsersWithWordsToRepeat();
        verify(telegramService, never()).processScheduledRepeat(anyLong());
    }

    @Test
    void repeatWords_ShouldBeAsync() {
        // Arrange
        Long userId = 456L;
        when(wordService.getUsersWithWordsToRepeat()).thenReturn(List.of(userId));
        doNothing().when(telegramService).processScheduledRepeat(anyLong());

        // Act
        repeatWordsService.repeatWords();

        // Assert
        verify(wordService, times(1)).getUsersWithWordsToRepeat();
    }

    @Test
    void repeatWords_ShouldPassCorrectParametersToTelegramService() {
        // Arrange
        Long userId1 = 456L;
        Long userId2 = 101L;
        List<Long> usersWithWords = List.of(userId1, userId2);

        when(wordService.getUsersWithWordsToRepeat()).thenReturn(usersWithWords);

        // Act
        repeatWordsService.repeatWords();

        // Assert
        verify(telegramService, times(2)).processScheduledRepeat(userIdCaptor.capture());

        List<Long> capturedUserIds = userIdCaptor.getAllValues();

        assertEquals(2, capturedUserIds.size());

        assertEquals(456L, capturedUserIds.get(0));
        assertEquals(101L, capturedUserIds.get(1));
    }
}
