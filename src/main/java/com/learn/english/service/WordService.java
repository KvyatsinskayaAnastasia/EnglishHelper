package com.learn.english.service;

import com.learn.english.model.DailyRepeatCount;
import com.learn.english.model.WordForRepeat;
import com.learn.english.model.WordState;
import lombok.NonNull;

import java.util.List;
import java.util.UUID;

public interface WordService {
    List<Long> getUsersWithWordsToRepeat();

    int countWordsForRepeat(Long userId);

    int countLearningWords(Long userId);

    List<DailyRepeatCount> countRepeatsForNextDays(Long userId, int days);

    List<WordForRepeat> getWordsForRepeatByRepeatAtIsLessThanEqualAndUserId(Long userId, Integer size);

    void addNewWord(WordState wordState, @NonNull Long userId);

    void updateRepeatedWord(UUID wordId, boolean isRepeatFailed);

    List<String> getRandomTranslationsByUserId(Long userId, UUID excludeWordId, Integer size);

    List<String> getRandomOriginalsByUserId(Long userId, UUID excludeWordId, Integer size);

    List<WordForRepeat> getRandomWordEOSByUserId(Long userId, Integer size);

    void moveLearnedWordsToArchive();
}
