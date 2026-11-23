package com.learn.english.service.impl;

import com.learn.english.entity.WordEO;
import com.learn.english.exception.DuplicateWordException;
import com.learn.english.mapper.WordMapper;
import com.learn.english.model.DailyRepeatCount;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaseWordService implements WordService {
    private static final int REPEATED_COUNT_FOR_LEARNED = 8;

    private final WordRepository wordRepository;
    private final WordMapper wordMapper;
    private final WordArchiveService wordArchiveService;

    @Override
    public List<Long> getUsersWithWordsToRepeat() {
        return wordRepository.findDistinctUsersWithWordsToRepeat(LocalDateTime.now());
    }

    @Override
    public int countWordsForRepeat(Long userId) {
        return wordRepository.countAllByRepeatAtIsLessThanEqualAndUserId(LocalDateTime.now(), userId);
    }

    @Override
    public int countLearningWords(Long userId) {
        return wordRepository.countByUserId(userId);
    }

    @Override
    public List<DailyRepeatCount> countRepeatsForNextDays(Long userId, int days) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            counts.put(today.plusDays(i), 0);
        }
        LocalDateTime until = today.plusDays(days).atStartOfDay();
        for (LocalDateTime repeatAt : wordRepository.findRepeatAtByUserIdAndRepeatAtBefore(userId, until)) {
            LocalDate day = repeatAt.toLocalDate();
            if (day.isBefore(today)) {
                day = today;
            }
            counts.computeIfPresent(day, (date, count) -> count + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new DailyRepeatCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<WordForRepeat> getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(Long userId, Integer size) {
        return wordRepository.findAllByRepeatAtIsLessThanEqualAndUserId(LocalDateTime.now(), userId,
                        PageRequest.of(0, size, Sort.by("repeatedCount").ascending()
                                .and(Sort.by("repeatAt").ascending()))).stream()
                .map(BaseWordService::toWordForRepeat)
                .toList();
    }

    @Override
    @Transactional
    public void addNewWord(WordState wordState, @NonNull Long userId) {
        if (wordRepository.existsByOriginalAndTranslationAndUserId(
                wordState.getOriginal(), wordState.getTranslation(), userId)) {
            throw new DuplicateWordException(wordState.getOriginal(), wordState.getTranslation());
        }
        WordEO wordEO = wordMapper.toEO(wordState, userId);
        wordEO.setRepeatAt(LocalDateTime.now());
        wordRepository.save(wordEO);
    }

    @Override
    @Transactional
    public void updateRepeatedWord(UUID wordId, boolean isRepeatFailed) {
        WordEO wordEO = wordRepository.findById(wordId).orElseThrow();
        wordEO.setRepeatedCount(isRepeatFailed
                ? Math.max(0, wordEO.getRepeatedCount() - 1)
                : wordEO.getRepeatedCount() + 1);
        wordEO.setRepeatAt(REPEATED_COUNT_FOR_LEARNED != wordEO.getRepeatedCount() ?
                LocalDateTime.now().plusDays(getDelayToNextRepeat(wordEO.getRepeatedCount()))
                : null);
        wordRepository.save(wordEO);
    }

    private int getDelayToNextRepeat(int repeatCount) {
        return repeatCount < 5 ? repeatCount : (repeatCount % 4) * 7;
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
                word.getUserId(), word.getId(), false);
    }

    private List<WordEO> getRandomWordEOSByUserId(Long userId, UUID excludeWordId, Integer size) {
        return wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(userId, excludeWordId, size);
    }
}
