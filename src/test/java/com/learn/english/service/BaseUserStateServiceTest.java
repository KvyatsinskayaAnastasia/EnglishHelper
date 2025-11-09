package com.learn.english.service;

import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.impl.BaseUserStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseUserStateServiceTest {

    @Mock
    private RedisTemplate<String, UserState> redisTemplate;

    @Mock
    private ValueOperations<String, UserState> valueOperations;

    @InjectMocks
    private BaseUserStateService userStateService;

    private static final Long TEST_USER_ID = 123L;
    private static final String EXPECTED_KEY = "user_state:123";

    @Test
    void getUserState_whenStateExists_shouldReturnExistingState() {
        // Arrange
        UserState expectedState = new UserState(TEST_USER_ID);
        expectedState.setUserStatus(UserStatus.FILLING_WORD);

        when(valueOperations.get(EXPECTED_KEY)).thenReturn(expectedState);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        UserState result = userStateService.getUserState(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(UserStatus.FILLING_WORD, result.getUserStatus());
        verify(valueOperations, times(1)).get(EXPECTED_KEY);
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void getUserState_whenStateNotExists_shouldCreateNewStateAndSave() {
        // Arrange
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        UserState result = userStateService.getUserState(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(UserStatus.NO_ACTIVITY, result.getUserStatus()); // Default status

        verify(valueOperations, times(1)).get(EXPECTED_KEY);
        verify(valueOperations, times(1)).set(eq(EXPECTED_KEY), any(UserState.class), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void saveUserState_shouldCallRedisTemplateWithCorrectParameters() {
        // Arrange
        UserState state = new UserState(TEST_USER_ID);
        state.setUserStatus(UserStatus.FILLING_WORD);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        userStateService.saveUserState(state);

        // Assert
        verify(valueOperations, times(1)).set(EXPECTED_KEY, state, 24L, TimeUnit.HOURS);
    }

    @Test
    void updateUserStatus_shouldUpdateStatusAndSave() {
        // Arrange
        UserState existingState = new UserState(TEST_USER_ID);
        existingState.setUserStatus(UserStatus.NO_ACTIVITY);

        when(valueOperations.get(EXPECTED_KEY)).thenReturn(existingState);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        userStateService.updateUserStatus(TEST_USER_ID, UserStatus.FILLING_WORD);

        // Assert
        verify(valueOperations, times(1)).get(EXPECTED_KEY);
        verify(valueOperations, times(1)).set(eq(EXPECTED_KEY), argThat(state ->
                state.getUserId().equals(TEST_USER_ID) &&
                        state.getUserStatus() == UserStatus.FILLING_WORD
        ), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void clearUserState_shouldResetStateToNoActivity() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        userStateService.clearUserState(TEST_USER_ID);

        // Assert
        verify(valueOperations, times(1)).set(eq(EXPECTED_KEY), argThat(state ->
                state.getUserId().equals(TEST_USER_ID) &&
                        state.getUserStatus() == UserStatus.NO_ACTIVITY
        ), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void deleteUserState_shouldCallRedisDelete() {
        // Act
        userStateService.deleteUserState(TEST_USER_ID);

        // Assert
        verify(redisTemplate, times(1)).delete(EXPECTED_KEY);
    }

    @Test
    void userExists_whenKeyExists_shouldReturnTrue() {
        // Arrange
        when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(true);

        // Act
        boolean result = userStateService.userExists(TEST_USER_ID);

        // Assert
        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey(EXPECTED_KEY);
    }

    @Test
    void userExists_whenKeyNotExists_shouldReturnFalse() {
        // Arrange
        when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(false);

        // Act
        boolean result = userStateService.userExists(TEST_USER_ID);

        // Assert
        assertFalse(result);
        verify(redisTemplate, times(1)).hasKey(EXPECTED_KEY);
    }

    @Test
    void extendUserStateTTL_whenKeyExists_shouldExtendTTL() {
        // Arrange
        when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(true);

        // Act
        userStateService.extendUserStateTTL(TEST_USER_ID);

        // Assert
        verify(redisTemplate, times(1)).hasKey(EXPECTED_KEY);
        verify(redisTemplate, times(1)).expire(EXPECTED_KEY, 24L, TimeUnit.HOURS);
    }

    @Test
    void extendUserStateTTL_whenKeyNotExists_shouldNotExtendTTL() {
        // Arrange
        when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(false);

        // Act
        userStateService.extendUserStateTTL(TEST_USER_ID);

        // Assert
        verify(redisTemplate, times(1)).hasKey(EXPECTED_KEY);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    void saveUserState_shouldHandleNullState() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> userStateService.saveUserState(null));
    }
}
