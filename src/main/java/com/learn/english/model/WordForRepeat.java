package com.learn.english.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
public class WordForRepeat implements Serializable {
    private String original;
    private String translation;
    private String exampleSentence;
    private Long userId;
    private UUID id;

    @Setter
    private boolean isRepeatFailed;
    private int countOfAttempts;

    public void increaseCountOfAttempts() {
        countOfAttempts++;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        WordForRepeat that = (WordForRepeat) o;
        return Objects.equals(original, that.original)
                && Objects.equals(translation, that.translation)
                && Objects.equals(exampleSentence, that.exampleSentence)
                && Objects.equals(userId, that.userId)
                && Objects.equals(id, that.id)
                && Objects.equals(isRepeatFailed, that.isRepeatFailed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(original, translation, exampleSentence, userId, id, isRepeatFailed);
    }
}
