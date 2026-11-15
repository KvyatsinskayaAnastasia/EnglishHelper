package com.learn.english.model;

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
public class RepeatingState implements Serializable {
    private List<WordForRepeat> repeatingWords;
    private WordForRepeat currentRepeatingWord;
    private List<String> repeatingLabels;
    private boolean isIncreaseRepeatingCount;
    private BotAction chosenRepeatingGame;
}
