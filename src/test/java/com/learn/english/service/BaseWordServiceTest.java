package com.learn.english.service;

import com.learn.english.entity.WordEO;
import com.learn.english.mapper.WordMapper;
import com.learn.english.model.WordForRepeat;
import com.learn.english.model.WordState;
import com.learn.english.repository.WordRepository;
import com.learn.english.service.impl.BaseWordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseWordServiceTest {

    @Mock
    private WordRepository wordRepository;

    @Mock
    private WordMapper wordMapper;

    @InjectMocks
    private BaseWordService wordService;

    private static final Long USER_ID = 1L;
    private static final UUID WORD_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    private WordEO wordEO;
    private WordState wordState;

    @BeforeEach
    void setUp() {
        wordEO = new WordEO();
        wordEO.setId(WORD_ID);
        wordEO.setUserId(USER_ID);
        wordEO.setOriginal("hello");
        wordEO.setTranslation("привет");
        wordEO.setExampleSentence("Hello world!");
        wordEO.setRepeatedCount(0);
        wordEO.setRepeatAt(NOW);

        wordState = new WordState();
        wordState.setOriginal("hello");
        wordState.setTranslation("привет");
        wordState.setExampleSentence("Hello world!");
    }

    @Test
    void getUsersWithWordsToRepeat_shouldReturnUserIds() {
        // Arrange
        when(wordRepository.findDistinctUsersWithWordsToRepeat(any()))
                .thenReturn(List.of(USER_ID, 2L));

        // Act
        List<Long> result = wordService.getUsersWithWordsToRepeat();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(USER_ID));
        verify(wordRepository).findDistinctUsersWithWordsToRepeat(any());
    }

    @Test
    void countWordsForRepeat_shouldReturnCount() {
        // Arrange
        when(wordRepository.countAllByRepeatAtIsLessThanEqualAndUserId(any(), eq(USER_ID)))
                .thenReturn(5);

        // Act
        int result = wordService.countWordsForRepeat(USER_ID);

        // Assert
        assertEquals(5, result);
        verify(wordRepository).countAllByRepeatAtIsLessThanEqualAndUserId(any(), eq(USER_ID));
    }

    @Test
    void getWordsForRepeat_shouldReturnWordsForRepeat() {
        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 10,
                Sort.by("repeatedCount").ascending().and(Sort.by("repeatAt").ascending()));

        when(wordRepository.findAllByRepeatAtIsLessThanEqualAndUserId(any(), eq(USER_ID), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(wordEO)));

        // Act
        List<WordForRepeat> result = wordService.getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(USER_ID, 10);

        // Assert
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).getOriginal());
        assertEquals("привет", result.get(0).getTranslation());
        verify(wordRepository).findAllByRepeatAtIsLessThanEqualAndUserId(any(), eq(USER_ID), any(PageRequest.class));
    }

    @Test
    void addNewWord_shouldSaveNewWord() {
        // Arrange
        when(wordMapper.toEO(wordState, USER_ID)).thenReturn(wordEO);

        // Act
        wordService.addNewWord(wordState, USER_ID);

        // Assert
        verify(wordMapper).toEO(wordState, USER_ID);
        verify(wordRepository).save(wordEO);
    }

    @Test
    void updateRepeatedWord_whenSuccess_shouldIncrementCountAndUpdateRepeatAt() {
        // Arrange
        wordEO.setRepeatedCount(2);
        when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(wordEO));

        // Act
        wordService.updateRepeatedWord(WORD_ID, false);

        // Assert
        assertEquals(3, wordEO.getRepeatedCount());
        assertTrue(wordEO.getRepeatAt().isAfter(NOW));
        verify(wordRepository).save(wordEO);
    }

    @Test
    void updateRepeatedWord_whenFailed_shouldDecrementCountAndUpdateRepeatAt() {
        // Arrange
        wordEO.setRepeatedCount(3);
        when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(wordEO));

        // Act
        wordService.updateRepeatedWord(WORD_ID, true);

        // Assert
        assertEquals(2, wordEO.getRepeatedCount());
        assertTrue(wordEO.getRepeatAt().isAfter(NOW));
        verify(wordRepository).save(wordEO);
    }

    @Test
    void updateRepeatedWord_whenFailedAndZeroCount_shouldNotGoBelowZero() {
        // Arrange
        wordEO.setRepeatedCount(0);
        when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(wordEO));

        // Act
        wordService.updateRepeatedWord(WORD_ID, true);

        // Assert
        assertEquals(0, wordEO.getRepeatedCount());
        verify(wordRepository).save(wordEO);
    }

    @Test
    void updateRepeatedWord_whenWordNotFound_shouldThrowException() {
        // Arrange
        when(wordRepository.findById(WORD_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                wordService.updateRepeatedWord(WORD_ID, false));
        verify(wordRepository, never()).save(any());
    }

    @Test
    void getRandomTranslationsByUserId_shouldReturnTranslations() {
        // Arrange
        when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, WORD_ID, 5))
                .thenReturn(List.of(wordEO));

        // Act
        List<String> result = wordService.getRandomTranslationsByUserId(USER_ID, WORD_ID, 5);

        // Assert
        assertEquals(1, result.size());
        assertEquals("привет", result.get(0));
        verify(wordRepository).getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, WORD_ID, 5);
    }

    @Test
    void getRandomTranslationsByUserId_whenNoWords_shouldReturnEmptyList() {
        // Arrange
        when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, WORD_ID, 5))
                .thenReturn(null);

        // Act
        List<String> result = wordService.getRandomTranslationsByUserId(USER_ID, WORD_ID, 5);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void getRandomOriginalsByUserId_shouldReturnOriginals() {
        // Arrange
        when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, WORD_ID, 5))
                .thenReturn(List.of(wordEO));

        // Act
        List<String> result = wordService.getRandomOriginalsByUserId(USER_ID, WORD_ID, 5);

        // Assert
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0));
    }

    @Test
    void getRandomWordEOSByUserId_shouldReturnWordForRepeatList() {
        // Arrange
        when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, null, 5))
                .thenReturn(List.of(wordEO));

        // Act
        List<WordForRepeat> result = wordService.getRandomWordEOSByUserId(USER_ID, 5);

        // Assert
        assertEquals(1, result.size());
        assertEquals(WORD_ID, result.get(0).getId());
        assertEquals("hello", result.get(0).getOriginal());
    }

    @Test
    void getRandomWordEOSByUserId_whenNoWords_shouldReturnEmptyList() {
        // Arrange
        when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, null, 5))
                .thenReturn(List.of());

        // Act
        List<WordForRepeat> result = wordService.getRandomWordEOSByUserId(USER_ID, 5);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void addNewWord_withNullUserId_shouldThrowException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                wordService.addNewWord(wordState, null));
        verify(wordRepository, never()).save(any());
    }

    @Test
    void addNewWord_withNullWordState_shouldThrowException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                wordService.addNewWord(null, USER_ID));
        verify(wordRepository, never()).save(any());
    }
}