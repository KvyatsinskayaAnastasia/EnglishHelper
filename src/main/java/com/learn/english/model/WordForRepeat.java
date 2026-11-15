package com.learn.english.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WordForRepeat implements Serializable {
    private String original;
    private String translation;
    private String exampleSentence;
    private Long userId;
    private UUID id;

    private boolean isRepeatFailed;
    private int countOfAttempts;

    public void increaseCountOfAttempts() {
        countOfAttempts++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WordForRepeat that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
