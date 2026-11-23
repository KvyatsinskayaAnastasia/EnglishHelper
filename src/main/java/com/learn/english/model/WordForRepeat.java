package com.learn.english.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordForRepeat implements Serializable {
    private String original;
    private String translation;
    private String exampleSentence;
    private Long userId;
    private UUID id;

    @Setter
    private boolean isRepeatFailed;

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
