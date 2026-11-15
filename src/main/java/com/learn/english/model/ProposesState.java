package com.learn.english.model;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProposesState implements Serializable {
    private List<String> translations;
    private List<String> examples;
}
