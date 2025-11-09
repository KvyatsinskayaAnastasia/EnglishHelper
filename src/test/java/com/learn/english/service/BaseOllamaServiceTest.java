package com.learn.english.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.english.service.impl.BaseOllamaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BaseOllamaServiceTest {

    @Mock
    private OllamaChatModel ollamaChatModel;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BaseOllamaService ollamaService;

    @Test
    void proposeTranslations_ShouldReturnTranslations_WhenOllamaReturnsValidResponse() throws JsonProcessingException {
        // Arrange
        String word = "hello";
        List<String> expectedTranslations = List.of("привет", "здравствуй");

        AssistantMessage translationMessage = new AssistantMessage("[\"привет\", \"здравствуй\"]");
        Generation translationGeneration = new Generation(translationMessage);
        ChatResponse successfulTranslationResponse = new ChatResponse(List.of(translationGeneration));

        when(ollamaChatModel.call(any(Prompt.class))).thenReturn(successfulTranslationResponse);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(expectedTranslations);

        // Act
        List<String> result = ollamaService.proposeTranslations(word);

        // Assert
        assertEquals(expectedTranslations, result);
        verify(ollamaChatModel, times(1)).call(any(Prompt.class));
        verify(objectMapper, times(1)).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void proposeTranslations_ShouldReturnEmptyList_WhenOllamaReturnsInvalidJson() throws JsonProcessingException {
        // Arrange
        String word = "hello";

        AssistantMessage translationMessage = new AssistantMessage("[\"привет\", \"здравствуй\"]");
        Generation translationGeneration = new Generation(translationMessage);
        ChatResponse successfulTranslationResponse = new ChatResponse(List.of(translationGeneration));

        when(ollamaChatModel.call(any(Prompt.class))).thenReturn(successfulTranslationResponse);
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

        // Act
        List<String> result = ollamaService.proposeTranslations(word);

        // Assert
        assertTrue(result.isEmpty());
        verify(ollamaChatModel, times(3)).call(any(Prompt.class));
        verify(objectMapper, times(3)).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void proposeTranslations_ShouldReturnEmptyList_WhenOllamaReturnsEmptyResponse() throws JsonProcessingException {
        // Arrange
        String word = "hello";

        AssistantMessage failedMessage = new AssistantMessage("");
        Generation failedGeneration = new Generation(failedMessage);
        ChatResponse failedChatResponse = new ChatResponse(List.of(failedGeneration));

        when(ollamaChatModel.call(any(Prompt.class))).thenReturn(failedChatResponse);

        // Act
        List<String> result = ollamaService.proposeTranslations(word);

        // Assert
        assertTrue(result.isEmpty());
        verify(ollamaChatModel, times(3)).call(any(Prompt.class));
        verify(objectMapper, never()).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void proposeTranslations_ShouldReturnEmptyList_WhenResultContainsBadSymbols() throws JsonProcessingException {
        // Arrange
        String word = "hello";

        AssistantMessage badMessage = new AssistantMessage("[\"bad1\", \"здравствуй\"]");
        Generation badGeneration = new Generation(badMessage);
        ChatResponse badChatResponse = new ChatResponse(List.of(badGeneration));

        when(ollamaChatModel.call(any(Prompt.class))).thenReturn(badChatResponse);

        // Act
        List<String> result = ollamaService.proposeTranslations(word);

        // Assert
        assertTrue(CollectionUtils.isEmpty(result));
        verify(ollamaChatModel, times(3)).call(any(Prompt.class));
        verify(objectMapper, never()).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void proposeTranslations_ShouldMakeThreeAttempts_WhenFirstTwoFail() throws JsonProcessingException {
        // Arrange
        String word = "hello";

        AssistantMessage failedMessage = new AssistantMessage("");
        Generation failedGeneration = new Generation(failedMessage);
        ChatResponse failedChatResponse = new ChatResponse(List.of(failedGeneration));

        AssistantMessage translationMessage = new AssistantMessage("[\"привет\", \"здравствуй\"]");
        Generation translationGeneration = new Generation(translationMessage);
        ChatResponse successfulTranslationResponse = new ChatResponse(List.of(translationGeneration));

        when(ollamaChatModel.call(any(Prompt.class)))
                .thenReturn(failedChatResponse)
                .thenReturn(failedChatResponse)
                .thenReturn(successfulTranslationResponse);

        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(List.of("успешный перевод"));

        // Act
        List<String> result = ollamaService.proposeTranslations(word);

        // Assert
        assertEquals(1, result.size());
        verify(ollamaChatModel, times(3)).call(any(Prompt.class));
        verify(objectMapper, times(1)).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void proposeTranslations_ShouldReturnEmptyList_WhenOllamaThrowsException() {
        // Arrange
        String word = "hello";

        when(ollamaChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Ollama error"));

        // Act
        List<String> result = ollamaService.proposeTranslations(word);

        // Assert
        assertTrue(result.isEmpty());
        verify(ollamaChatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void proposeExamples_ShouldReturnExamples_WhenOllamaReturnsValidResponse() throws JsonProcessingException {
        // Arrange
        String word = "run";
        String context = "бег";
        List<String> expectedExamples = List.of("I run every morning", "He runs fast");

        AssistantMessage exampleMessage = new AssistantMessage("[\"I run every morning\", \"He runs fast\"]");
        Generation exampleGeneration = new Generation(exampleMessage);
        ChatResponse successfulExampleResponse = new ChatResponse(List.of(exampleGeneration));

        when(ollamaChatModel.call(any(Prompt.class))).thenReturn(successfulExampleResponse);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(expectedExamples);

        // Act
        List<String> result = ollamaService.proposeExamples(word, context);

        // Assert
        assertEquals(expectedExamples, result);
        verify(ollamaChatModel, times(1)).call(any(Prompt.class));
        verify(objectMapper, times(1)).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void proposeExamples_ShouldReturnEmptyList_WhenResultContainsBadSymbols() throws JsonProcessingException {
        // Arrange
        String word = "run";
        String context = "бег";
        String badResponse = "Пример на русском языке";

        AssistantMessage badMessage = new AssistantMessage(badResponse);
        Generation badGeneration = new Generation(badMessage);
        ChatResponse badChatResponse = new ChatResponse(List.of(badGeneration));

        when(ollamaChatModel.call(any(Prompt.class))).thenReturn(badChatResponse);

        // Act
        List<String> result = ollamaService.proposeExamples(word, context);

        // Assert
        assertTrue(CollectionUtils.isEmpty(result));
        verify(ollamaChatModel, times(3)).call(any(Prompt.class)); // 3 попытки
        verify(objectMapper, never()).readValue(anyString(), any(TypeReference.class));
    }
}
