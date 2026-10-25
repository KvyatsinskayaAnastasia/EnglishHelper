package com.learn.english.service;

import com.learn.english.entity.WordEO;
import com.learn.english.mapper.WordMapper;
import com.learn.english.model.WordForRepeat;
import com.learn.english.model.WordState;
import com.learn.english.repository.WordRepository;
import com.learn.english.service.impl.BaseWordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    private static final Long USER_ID = 42L;
    private static final UUID WORD_ID = UUID.randomUUID();
    private static final int LEARNED_COUNT = 8;

    @Mock
    private WordRepository wordRepository;

    @Mock
    private WordMapper wordMapper;

    @InjectMocks
    private BaseWordService service;

    @Nested
    @DisplayName("getUsersWithWordsToRepeat")
    class GetUsersWithWordsToRepeatTests {

        @Test
        @DisplayName("delegates to repository with current time")
        void getUsersWithWordsToRepeat_delegates() {
            when(wordRepository.findDistinctUsersWithWordsToRepeat(any(LocalDateTime.class)))
                    .thenReturn(List.of(USER_ID));

            LocalDateTime before = LocalDateTime.now();
            List<Long> users = service.getUsersWithWordsToRepeat();
            LocalDateTime after = LocalDateTime.now();

            assertThat(users).containsExactly(USER_ID);
            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(wordRepository).findDistinctUsersWithWordsToRepeat(captor.capture());
            assertThat(captor.getValue()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("countWordsForRepeat")
    class CountWordsForRepeatTests {

        @Test
        @DisplayName("counts words with repeat_at <= now for the user")
        void countWordsForRepeat_delegates() {
            when(wordRepository.countAllByRepeatAtIsLessThanEqualAndUserId(any(LocalDateTime.class), eq(USER_ID)))
                    .thenReturn(7);

            assertThat(service.countWordsForRepeat(USER_ID)).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId")
    class GetWordsForRepeatTests {

        @Test
        @DisplayName("maps page to WordForRepeat with failed=false and 0 attempts, sorted by repeatedCount then repeatAt")
        void getWordsForRepeat_mapsAndUsesSort() {
            WordEO word = word("cat", "кот", "I have a cat");
            when(wordRepository.findAllByRepeatAtIsLessThanEqualAndUserId(
                    any(LocalDateTime.class), eq(USER_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(word)));

            List<WordForRepeat> result =
                    service.getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(USER_ID, 10);

            assertThat(result).hasSize(1);
            WordForRepeat mapped = result.get(0);
            assertThat(mapped.getOriginal()).isEqualTo("cat");
            assertThat(mapped.getTranslation()).isEqualTo("кот");
            assertThat(mapped.getExampleSentence()).isEqualTo("I have a cat");
            assertThat(mapped.getUserId()).isEqualTo(USER_ID);
            assertThat(mapped.getId()).isEqualTo(WORD_ID);
            assertThat(mapped.isRepeatFailed()).isFalse();
            assertThat(mapped.getCountOfAttempts()).isZero();

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(wordRepository).findAllByRepeatAtIsLessThanEqualAndUserId(
                    any(LocalDateTime.class), eq(USER_ID), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort()).isEqualTo(
                    Sort.by("repeatedCount").ascending().and(Sort.by("repeatAt").ascending()));
        }
    }

    @Nested
    @DisplayName("addNewWord")
    class AddNewWordTests {

        @Test
        @DisplayName("maps state to entity, sets repeatAt to now and saves")
        void addNewWord_setsRepeatAtAndSaves() {
            WordState state = new WordState("cat", "кот", "I have a cat");
            WordEO entity = new WordEO();
            when(wordMapper.toEO(state, USER_ID)).thenReturn(entity);

            LocalDateTime before = LocalDateTime.now();
            service.addNewWord(state, USER_ID);
            LocalDateTime after = LocalDateTime.now();

            assertThat(entity.getRepeatAt()).isBetween(before, after);
            verify(wordRepository).save(entity);
        }

        @Test
        @DisplayName("rejects null userId")
        void addNewWord_nullUserId_throws() {
            WordState state = new WordState("cat", "кот", "I have a cat");

            assertThatThrownBy(() -> service.addNewWord(state, null))
                    .isInstanceOf(NullPointerException.class);
            verifyNoMoreInteractions(wordRepository);
        }
    }

    @Nested
    @DisplayName("updateRepeatedWord")
    class UpdateRepeatedWordTests {

        @Test
        @DisplayName("successful answer increments count and schedules delay equal to the new count when count < 5")
        void updateRepeatedWord_successBelowFive_incrementsAndDelaysByCount() {
            WordEO word = existingWord(0);
            when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(word));

            LocalDateTime before = LocalDateTime.now();
            service.updateRepeatedWord(WORD_ID, false);

            assertThat(word.getRepeatedCount()).isEqualTo(1);
            assertThat(word.getRepeatAt()).isCloseTo(before.plusDays(1), within(2, java.time.temporal.ChronoUnit.SECONDS));
            verify(wordRepository).save(word);
        }

        @Test
        @DisplayName("successful answer at count 4 sets delay to 7 days (5 % 4 * 7)")
        void updateRepeatedWord_successToFive_usesWeeklyDelay() {
            WordEO word = existingWord(4);
            when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(word));

            LocalDateTime before = LocalDateTime.now();
            service.updateRepeatedWord(WORD_ID, false);

            assertThat(word.getRepeatedCount()).isEqualTo(5);
            assertThat(word.getRepeatAt()).isCloseTo(before.plusDays(7), within(2, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("successful answer to 6/7 uses 14 and 21 day delays")
        void updateRepeatedWord_successToSixAndSeven_usesTwoAndThreeWeeks() {
            WordEO six = existingWord(5);
            when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(six));
            LocalDateTime before = LocalDateTime.now();
            service.updateRepeatedWord(WORD_ID, false);
            assertThat(six.getRepeatedCount()).isEqualTo(6);
            assertThat(six.getRepeatAt()).isCloseTo(before.plusDays(14), within(2, java.time.temporal.ChronoUnit.SECONDS));

            WordEO seven = existingWord(6);
            when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(seven));
            before = LocalDateTime.now();
            service.updateRepeatedWord(WORD_ID, false);
            assertThat(seven.getRepeatedCount()).isEqualTo(7);
            assertThat(seven.getRepeatAt()).isCloseTo(before.plusDays(21), within(2, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("successful answer that reaches 8 marks the word learned (repeatAt = null)")
        void updateRepeatedWord_successToLearned_clearsRepeatAt() {
            WordEO word = existingWord(7);
            when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(word));

            service.updateRepeatedWord(WORD_ID, false);

            assertThat(word.getRepeatedCount()).isEqualTo(LEARNED_COUNT);
            assertThat(word.getRepeatAt()).isNull();
            verify(wordRepository).save(word);
        }

        @Test
        @DisplayName("failed answer decrements count but not below zero")
        void updateRepeatedWord_failed_decrementsWithFloorZero() {
            WordEO word = existingWord(0);
            when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(word));

            service.updateRepeatedWord(WORD_ID, true);

            assertThat(word.getRepeatedCount()).isZero();
            assertThat(word.getRepeatAt()).isNotNull();
        }

        @Test
        @DisplayName("failed answer from 8 sets count to 7 and 21-day delay")
        void updateRepeatedWord_failedFromLearned_schedulesThreeWeeks() {
            WordEO word = existingWord(8);
            when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(word));

            LocalDateTime before = LocalDateTime.now();
            service.updateRepeatedWord(WORD_ID, true);

            assertThat(word.getRepeatedCount()).isEqualTo(7);
            assertThat(word.getRepeatAt()).isCloseTo(before.plusDays(21), within(2, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("throws when word id is unknown")
        void updateRepeatedWord_missing_throws() {
            when(wordRepository.findById(WORD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateRepeatedWord(WORD_ID, false))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("random distractors")
    class RandomDistractorTests {

        @Test
        @DisplayName("getRandomTranslationsByUserId maps translations and returns empty list on null page")
        void getRandomTranslations_mapsOrEmpty() {
            WordEO word = word("dog", "собака", "I haven't a dog");
            when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, WORD_ID, 3))
                    .thenReturn(List.of(word));

            assertThat(service.getRandomTranslationsByUserId(USER_ID, WORD_ID, 3))
                    .containsExactly("собака");

            when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, WORD_ID, 3))
                    .thenReturn(null);
            assertThat(service.getRandomTranslationsByUserId(USER_ID, WORD_ID, 3)).isEmpty();
        }

        @Test
        @DisplayName("getRandomOriginalsByUserId maps originals and returns empty list on empty page")
        void getRandomOriginals_mapsOrEmpty() {
            WordEO word = word("dog", "собака", "I haven't a dog");
            when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, WORD_ID, 3))
                    .thenReturn(List.of(word));

            assertThat(service.getRandomOriginalsByUserId(USER_ID, WORD_ID, 3))
                    .containsExactly("dog");

            when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, WORD_ID, 3))
                    .thenReturn(Collections.emptyList());
            assertThat(service.getRandomOriginalsByUserId(USER_ID, WORD_ID, 3)).isEmpty();
        }

        @Test
        @DisplayName("getRandomWordEOSByUserId maps WordForRepeat without exclude id")
        void getRandomWords_maps() {
            WordEO word = word("dog", "собака", "I haven't a dog");
            when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, null, 10))
                    .thenReturn(List.of(word));

            List<WordForRepeat> result = service.getRandomWordEOSByUserId(USER_ID, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginal()).isEqualTo("dog");
            assertThat(result.get(0).getId()).isEqualTo(WORD_ID);
            assertThat(result.get(0).isRepeatFailed()).isFalse();
        }

        @Test
        @DisplayName("getRandomWordEOSByUserId returns empty list when repository has no rows")
        void getRandomWords_empty() {
            when(wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(USER_ID, null, 10))
                    .thenReturn(Collections.emptyList());

            assertThat(service.getRandomWordEOSByUserId(USER_ID, 10)).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteLearnedWords")
    class DeleteLearnedWordsTests {

        @Test
        @DisplayName("loads words with repeatedCount >= 8, deletes them and returns the list")
        void deleteLearnedWords_deletesAndReturns() {
            WordEO learned = existingWord(LEARNED_COUNT);
            when(wordRepository.findWordEOSByRepeatedCountGreaterThanEqual(LEARNED_COUNT))
                    .thenReturn(List.of(learned));

            List<WordEO> result = service.deleteLearnedWords();

            assertThat(result).containsExactly(learned);
            verify(wordRepository).deleteAll(List.of(learned));
        }
    }

    private static WordEO existingWord(int repeatedCount) {
        WordEO word = new WordEO();
        word.setId(WORD_ID);
        word.setRepeatedCount(repeatedCount);
        word.setUserId(USER_ID);
        return word;
    }

    private static WordEO word(String original, String translation, String example) {
        WordEO word = existingWord(0);
        word.setOriginal(original);
        word.setTranslation(translation);
        word.setExampleSentence(example);
        return word;
    }
}
