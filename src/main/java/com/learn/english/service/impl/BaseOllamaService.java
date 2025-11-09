package com.learn.english.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.english.builder.PromptBuilder;
import com.learn.english.model.PromptType;
import com.learn.english.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaseOllamaService implements OllamaService {
    private static final int MAX_ATTEMPTS = 3;

    private final OllamaChatModel ollamaChatModel;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> proposeTranslations(String word) {
        return proposeByPrompt(PromptType.TRANSLATE_WORD_TO_RUSSIAN,
                List.of(word), ".*[a-zA-Z].*");
    }

    @Override
    public List<String> proposeExamples(String word, String context) {
        return proposeByPrompt(PromptType.PROPOSE_EXAMPLES_OF_USING,
                List.of(word, context), ".*[а-яА-Я].*");
    }

    private List<String> proposeByPrompt(PromptType promptType, List<String> fillers, String badSymbolsRegex) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                String result = callOllama(promptType, fillers, attempt);
                if (isValidResult(result, badSymbolsRegex)) {
                    return parseResult(result);
                }
            } catch (JsonProcessingException e) {
                log.warn("JSON parsing failed on attempt {}: {}", attempt + 1, e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error on attempt {}: {}", attempt + 1, e.getMessage());
                break;
            }
        }
        return Collections.emptyList();
    }

    private String callOllama(PromptType promptType, List<String> fillers, int attempt) {
        try {
            return ollamaChatModel.call(new PromptBuilder(promptType, fillers).build())
                    .getResult().getOutput().getText();
        } catch (Exception e) {
            log.info("ollama called: {}", ollamaChatModel.getDefaultOptions().getModel());
            log.warn("Ollama call failed on attempt {}: {}", attempt + 1, e.getMessage());
            throw e;
        }
    }

    private boolean isValidResult(String result, String badSymbolsRegex) {
        return StringUtils.isNoneBlank(result) && !result.matches(badSymbolsRegex);
    }

    private List<String> parseResult(String result) throws JsonProcessingException {
        return objectMapper.readValue(result, new TypeReference<>() {});
    }
}