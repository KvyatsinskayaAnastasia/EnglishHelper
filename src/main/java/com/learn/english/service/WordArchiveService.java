package com.learn.english.service;

import com.learn.english.entity.WordEO;
import lombok.NonNull;

import java.util.List;

public interface WordArchiveService {
    void saveArchiveWords(@NonNull List<WordEO> wordEOList);
}
