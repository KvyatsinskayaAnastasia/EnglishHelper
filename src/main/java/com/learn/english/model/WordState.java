package com.learn.english.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
public class WordState implements Serializable {
    private String original;
    private String translation;
    private String exampleSentence;

    @Override
    public String toString() {
        return "WordState{" +
                "original='" + original + '\'' +
                ", translation='" + translation + '\'' +
                ", exampleSentence='" + exampleSentence + '\'' +
                '}';
    }
}
