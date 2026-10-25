package com.learn.english.service.impl;

import com.learn.english.model.UserState;
import com.learn.english.service.UserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BaseUserStateService implements UserStateService {
    private static final String KEY_PREFIX = "user_state:";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, UserState> redisTemplate;

    @Override
    public UserState getUserState(Long userId) {
        String key = getKey(userId);
        UserState state = redisTemplate.opsForValue().get(key);
        if (state == null) {
            state = new UserState(userId);
            saveUserState(state);
        }
        return state;
    }

    @Override
    public void saveUserState(UserState state) {
        String key = getKey(state.getUserId());
        redisTemplate.opsForValue().set(key, state, TTL_HOURS, TimeUnit.HOURS);
    }

    private String getKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
