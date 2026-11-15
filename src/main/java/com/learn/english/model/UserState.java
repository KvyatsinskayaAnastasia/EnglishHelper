package com.learn.english.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UserState implements Serializable {
    @NonNull
    private UserStatus userStatus;
    private WordState currentWordState;
    private ProposesState proposesState;
    private RepeatingState repeatingState;
    private Integer lastBotCommandMessageId;

    private Long userId;

    protected UserState() {}

    public UserState(Long userId) {
        this.userStatus = UserStatus.NO_ACTIVITY;
        this.userId = userId;
    }
}
