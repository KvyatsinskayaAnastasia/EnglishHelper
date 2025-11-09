package com.learn.english.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "words_archive")
public class WordsArchiveEO {
    @Id
    @GeneratedValue
    @UuidGenerator
    UUID id;

    public WordsArchiveEO(String original, String translation, String exampleSentence, Long userId, LocalDateTime createdAt) {
        this.original = original;
        this.translation = translation;
        this.exampleSentence = exampleSentence;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    protected WordsArchiveEO() {}

    @Column(nullable = false)
    String original;

    @Column(nullable = false)
    String translation;

    @Column(name = "example_sentence")
    String exampleSentence;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
