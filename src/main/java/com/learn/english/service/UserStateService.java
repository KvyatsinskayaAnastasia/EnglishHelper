package com.learn.english.service;

import com.learn.english.model.UserState;

public interface UserStateService {

    UserState getUserState(Long userId);

    void saveUserState(UserState state);
}
