package com.learn.english.service;

import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.impl.BaseUserStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStateServiceTest {

    private static final Long USER_ID = 12345L;
    private static final String REDIS_KEY = "user_state:12345";

    @Mock
    private RedisTemplate<String, UserState> redisTemplate;

    @Mock
    private ValueOperations<String, UserState> valueOperations;

    @InjectMocks
    private BaseUserStateService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("getUserState")
    class GetUserStateTests {

        @Test
        @DisplayName("returns cached state when Redis has a value")
        void getUserState_existing_returnsCached() {
            UserState cached = new UserState(USER_ID);
            cached.setUserStatus(UserStatus.FILLING_ORIGINAL_WITHOUT_BOT);
            when(valueOperations.get(REDIS_KEY)).thenReturn(cached);

            UserState result = service.getUserState(USER_ID);

            assertThat(result).isSameAs(cached);
            assertThat(result.getUserStatus()).isEqualTo(UserStatus.FILLING_ORIGINAL_WITHOUT_BOT);
        }

        @Test
        @DisplayName("creates NO_ACTIVITY state and saves it with 24h TTL when key is missing")
        void getUserState_missing_createsDefaultAndSaves() {
            when(valueOperations.get(REDIS_KEY)).thenReturn(null);

            UserState result = service.getUserState(USER_ID);

            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);

            ArgumentCaptor<UserState> captor = ArgumentCaptor.forClass(UserState.class);
            verify(valueOperations).set(eq(REDIS_KEY), captor.capture(), eq(24L), eq(TimeUnit.HOURS));
            assertThat(captor.getValue()).isSameAs(result);
        }
    }

    @Nested
    @DisplayName("saveUserState")
    class SaveUserStateTests {

        @Test
        @DisplayName("writes state to user_state:{userId} with 24 hour TTL")
        void saveUserState_setsKeyWithTtl() {
            UserState state = new UserState(USER_ID);

            service.saveUserState(state);

            verify(valueOperations).set(REDIS_KEY, state, 24L, TimeUnit.HOURS);
        }
    }
}
