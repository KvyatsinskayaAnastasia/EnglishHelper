package com.learn.english.service.impl;

import com.learn.english.entity.WordEO;
import com.learn.english.exception.DuplicateWordException;
import com.learn.english.mapper.WordMapper;
import com.learn.english.model.WordForRepeat;
import com.learn.english.model.WordState;
import com.learn.english.repository.WordRepository;
import com.learn.english.service.WordArchiveService;
import com.learn.english.service.WordService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaseWordService implements WordService {
    private static final int REPEATED_COUNT_FOR_LEARNED = 8;

    private final WordRepository wordRepository;
    private final WordMapper wordMapper;
    private final WordArchiveService wordArchiveService;
    private final Clock clock;

    @Override
    public List<Long> getUsersWithWordsToRepeat() {
        return wordRepository.findDistinctUsersWithWordsToRepeat(now());
    }

    @Override
    public int countWordsForRepeat(Long userId) {
        return wordRepository.countAllByRepeatAtIsLessThanEqualAndUserId(now(), userId);
    }

    @Override
    public List<WordForRepeat> getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(Long userId, Integer size) {
        return wordRepository.findAllByRepeatAtIsLessThanEqualAndUserId(now(), userId,
                        PageRequest.of(0, size, Sort.by("repeatedCount").ascending()
                                .and(Sort.by("repeatAt").ascending()))).stream()
                .map(BaseWordService::toWordForRepeat)
                .toList();
    }

    @Override
    @Transactional
    public void addNewWord(WordState wordState, @NonNull Long userId) {
        wordState.setOriginal(trimToNull(wordState.getOriginal()));
        wordState.setTranslation(trimToNull(wordState.getTranslation()));
        wordState.setExampleSentence(trimToNull(wordState.getExampleSentence()));
        if (wordState.getOriginal() == null || wordState.getTranslation() == null) {
            throw new IllegalArgumentException("Original and translation are required");
        }
        if (wordRepository.existsByOriginalAndTranslationAndUserId(
                wordState.getOriginal(), wordState.getTranslation(), userId)) {
            throw new DuplicateWordException(wordState.getOriginal(), wordState.getTranslation());
        }
        WordEO wordEO = wordMapper.toEO(wordState, userId);
        wordEO.setRepeatAt(now());
        wordRepository.save(wordEO);
    }

    @Override
    @Transactional
    public void updateRepeatedWord(UUID wordId, @NonNull Long userId, boolean isRepeatFailed) {
        WordEO wordEO = wordRepository.findById(wordId)
                .filter(word -> userId.equals(word.getUserId()))
                .orElseThrow();
        int repeatedCount = nextRepeatedCount(wordEO.getRepeatedCount(), isRepeatFailed);
        wordEO.setRepeatedCount(repeatedCount);
        wordEO.setRepeatAt(repeatedCount == REPEATED_COUNT_FOR_LEARNED
                ? null
                : now().plusDays(delayDays(repeatedCount)));
        wordRepository.save(wordEO);
    }

    private static int nextRepeatedCount(int repeatedCount, boolean repeatFailed) {
        return repeatFailed ? Math.max(0, repeatedCount - 1) : repeatedCount + 1;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static int delayDays(int repeatedCount) {
        return repeatedCount < 5 ? repeatedCount : (repeatedCount % 4) * 7;
    }

    @Override
    public List<String> getRandomTranslationsByUserId(Long userId, UUID excludeWordId, Integer size) {
        List<WordEO> randomPage = getRandomWordEOSByUserId(userId, excludeWordId, size);
        return randomPage != null ? randomPage.stream()
                .map(WordEO::getTranslation)
                .toList() : Collections.emptyList();
    }

    @Override
    public List<String> getRandomOriginalsByUserId(Long userId, UUID excludeWordId, Integer size) {
        List<WordEO> randomPage = getRandomWordEOSByUserId(userId, excludeWordId, size);
        return !CollectionUtils.isEmpty(randomPage) ? randomPage.stream()
                .map(WordEO::getOriginal)
                .toList() : Collections.emptyList();
    }

    @Override
    public List<WordForRepeat> getRandomWordEOSByUserId(Long userId, Integer size) {
        List<WordEO> randomPage = wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(userId, null, size);
        return !CollectionUtils.isEmpty(randomPage) ? randomPage.stream()
                .map(BaseWordService::toWordForRepeat)
                .toList() : Collections.emptyList();
    }

    @Override
    @Transactional
    public void moveLearnedWordsToArchive() {
        List<WordEO> learnedWords = findLearnedWords();
        if (CollectionUtils.isEmpty(learnedWords)) {
            return;
        }
        wordArchiveService.saveArchiveWords(learnedWords);
        wordRepository.deleteAll(learnedWords);
    }

    private List<WordEO> findLearnedWords() {
        return wordRepository.findWordEOSByRepeatedCountGreaterThanEqual(REPEATED_COUNT_FOR_LEARNED);
    }

    private static WordForRepeat toWordForRepeat(WordEO word) {
        return new WordForRepeat(word.getOriginal(), word.getTranslation(), word.getExampleSentence(),
                word.getUserId(), word.getId(), false, 0);
    }

    private List<WordEO> getRandomWordEOSByUserId(Long userId, UUID excludeWordId, Integer size) {
        return wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(userId, excludeWordId, size);
    }
}
