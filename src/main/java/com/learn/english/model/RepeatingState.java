package com.learn.english.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
public class RepeatingState implements Serializable {
    private List<WordForRepeat> repeatingWords;
    private WordForRepeat currentRepeatingWord;
    private List<String> repeatingLabels;
    private boolean isIncreaseRepeatingCount;
}
