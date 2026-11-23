package com.learn.english.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

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
    private BotAction chosenRepeatingGame;

    public void setRepeatingWords(List<WordForRepeat> repeatingWords) {
        this.repeatingWords = repeatingWords;
        this.currentRepeatingWord = queuedWord(this.currentRepeatingWord);
    }

    public void setCurrentRepeatingWord(WordForRepeat currentRepeatingWord) {
        this.currentRepeatingWord = queuedWord(currentRepeatingWord);
    }

    public List<WordForRepeat> candidatesExceptCurrent() {
        if (currentRepeatingWord == null || repeatingWords == null || repeatingWords.size() < 2) {
            return repeatingWords;
        }
        List<WordForRepeat> others = repeatingWords.stream()
                .filter(word -> !Objects.equals(currentRepeatingWord.getId(), word.getId()))
                .toList();
        return others.isEmpty() ? repeatingWords : others;
    }

    // Redis stores the queue and the current card as two objects. Answers must update the queued one.
    private WordForRepeat queuedWord(WordForRepeat card) {
        if (card == null || repeatingWords == null) {
            return card;
        }
        for (WordForRepeat queued : repeatingWords) {
            if (queued == card) {
                return queued;
            }
            if (queued != null && Objects.equals(queued.getId(), card.getId())) {
                if (card.isRepeatFailed()) {
                    queued.setRepeatFailed(true);
                }
                return queued;
            }
        }
        return card;
    }
}
