package com.learn.english.service;

import java.util.List;

public interface OllamaService {
    List<String> proposeTranslations(String word);

    List<String> proposeExamples(String word, String context);
}
