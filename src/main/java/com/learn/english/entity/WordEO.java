package com.learn.english.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "word")
public class WordEO {
    @Id
    @GeneratedValue
    @UuidGenerator
    UUID id;

    @Column(nullable = false)
    String original;

    @Column(nullable = false)
    String translation;

    @Column(name = "example_sentence")
    String exampleSentence;

    @Column(name = "repeated_count", nullable = false)
    int repeatedCount;

    @Column(name = "repeat_at", nullable = false)
    LocalDateTime repeatAt;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "created_at")
    @CreationTimestamp
    LocalDateTime createdAt;
}
