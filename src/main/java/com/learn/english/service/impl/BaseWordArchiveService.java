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
        wordArchiveRepository.saveAll(wordEOList.stream().map(w ->
                new WordsArchiveEO(w.getOriginal(), w.getTranslation(), w.getExampleSentence(), w.getUserId(), w.getCreatedAt())).toList());
    }
}
