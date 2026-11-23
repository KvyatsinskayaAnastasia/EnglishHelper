package com.learn.english.service.impl;

import com.learn.english.entity.WordEO;
import com.learn.english.entity.WordsArchiveEO;
import com.learn.english.repository.WordArchiveRepository;
import com.learn.english.service.WordArchiveService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseWordArchiveService implements WordArchiveService {
    private final WordArchiveRepository wordArchiveRepository;

    @Override
    @Transactional
    public void saveArchiveWords(@NonNull List<WordEO> wordEOList) {
        wordArchiveRepository.saveAll(wordEOList.stream().map(BaseWordArchiveService::toArchive).toList());
    }

    @Override
    public int countArchivedWords(@NonNull Long userId) {
        return wordArchiveRepository.countByUserId(userId);
    }

    private static WordsArchiveEO toArchive(WordEO word) {
        return new WordsArchiveEO(word.getOriginal(), word.getTranslation(), word.getExampleSentence(),
                word.getUserId(), word.getCreatedAt());
    }
}
