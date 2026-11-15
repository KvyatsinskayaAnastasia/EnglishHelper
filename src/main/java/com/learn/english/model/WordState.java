package com.learn.english.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
