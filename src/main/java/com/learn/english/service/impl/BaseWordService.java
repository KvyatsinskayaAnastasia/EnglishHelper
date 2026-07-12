package com.learn.english.service.impl;

import com.learn.english.entity.WordEO;
import com.learn.english.mapper.WordMapper;
import com.learn.english.model.WordForRepeat;
import com.learn.english.model.WordState;
import com.learn.english.repository.WordRepository;
import com.learn.english.service.WordService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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

    @Override
    public List<Long> getUsersWithWordsToRepeat() {
        return wordRepository.findDistinctUsersWithWordsToRepeat(LocalDateTime.now());
    }

    @Override
    public int countWordsForRepeat(Long userId) {
        return wordRepository.countAllByRepeatAtIsLessThanEqualAndUserId(LocalDateTime.now(), userId);
    }

    @Override
    public List<WordForRepeat> getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(Long userId, Integer size) {
        return wordRepository.findAllByRepeatAtIsLessThanEqualAndUserId(LocalDateTime.now(), userId,
                        PageRequest.of(0, size, Sort.by("repeatedCount").ascending()
                                .and(Sort.by("repeatAt").ascending()))).stream()
                .map(word -> new WordForRepeat(word.getOriginal(), word.getTranslation(),
                        word.getExampleSentence(), word.getUserId(), word.getId(), false, 0))
                .toList();
    }

    @Override
    @Transactional
    public void addNewWord(WordState wordState, @NonNull Long userId) {
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
                .map(word -> new WordForRepeat(word.getOriginal(), word.getTranslation(),
                        word.getExampleSentence(), word.getUserId(), word.getId(), false, 0))
                .toList() : Collections.emptyList();
    }

    @Override
    @Transactional
    public List<WordEO> deleteLearnedWords() {
        var learnedWords = wordRepository.findWordEOSByRepeatedCountGreaterThanEqual(REPEATED_COUNT_FOR_LEARNED);
        wordRepository.deleteAll(learnedWords);
        return learnedWords;
    }

    private List<WordEO> getRandomWordEOSByUserId(Long userId, UUID excludeWordId, Integer size) {
        return wordRepository.getRandomWordEOSByUserIdExcludeWordWithId(userId, excludeWordId, size);
    }


}
