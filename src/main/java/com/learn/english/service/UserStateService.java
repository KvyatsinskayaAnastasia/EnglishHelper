package com.learn.english.service;

import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;

public interface UserStateService {

    UserState getUserState(Long userId);

    void saveUserState(UserState state);

    void updateUserStatus(Long userId, UserStatus status);

    void clearUserState(Long userId);

    void deleteUserState(Long userId);

    boolean userExists(Long userId);

    void extendUserStateTTL(Long userId);
}
