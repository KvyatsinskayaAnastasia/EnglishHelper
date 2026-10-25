package com.learn.english.service;

import com.learn.english.entity.WordEO;
import com.learn.english.entity.WordsArchiveEO;
import com.learn.english.repository.WordArchiveRepository;
import com.learn.english.service.impl.BaseWordArchiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WordArchiveServiceTest {

    @Mock
    private WordArchiveRepository wordArchiveRepository;

    @InjectMocks
    private BaseWordArchiveService service;

    @Test
    @DisplayName("maps WordEO fields onto WordsArchiveEO and saves the collection")
    void saveArchiveWords_mapsAndSaves() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        WordEO word = new WordEO();
        word.setOriginal("cat");
        word.setTranslation("кот");
        word.setExampleSentence("I have a cat");
        word.setUserId(7L);
        word.setCreatedAt(createdAt);

        service.saveArchiveWords(List.of(word));

        ArgumentCaptor<List<WordsArchiveEO>> captor = ArgumentCaptor.forClass(List.class);
        verify(wordArchiveRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        WordsArchiveEO archived = captor.getValue().get(0);
        assertThat(archived.getOriginal()).isEqualTo("cat");
        assertThat(archived.getTranslation()).isEqualTo("кот");
        assertThat(archived.getExampleSentence()).isEqualTo("I have a cat");
        assertThat(archived.getUserId()).isEqualTo(7L);
        assertThat(archived.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("saves an empty list when there are no learned words")
    void saveArchiveWords_emptyList() {
        service.saveArchiveWords(List.of());

        ArgumentCaptor<List<WordsArchiveEO>> captor = ArgumentCaptor.forClass(List.class);
        verify(wordArchiveRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("rejects a null list")
    void saveArchiveWords_null_throws() {
        assertThatThrownBy(() -> service.saveArchiveWords(null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(wordArchiveRepository);
    }
}
