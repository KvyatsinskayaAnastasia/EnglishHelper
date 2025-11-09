package com.learn.english.builder;

import com.learn.english.model.PromptType;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

public class PromptBuilder {
    private final PromptType promptType;
    private final List<String> fillers;

    public PromptBuilder(PromptType promptType, List<String> fillers) {
        this.promptType = promptType;
        this.fillers = fillers;
    }

    public Prompt build() {
        return new Prompt(String.format(promptType.getPrompt(), fillers.toArray()));
    }
}
