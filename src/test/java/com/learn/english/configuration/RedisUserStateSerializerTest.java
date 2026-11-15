package com.learn.english.configuration;

import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisUserStateSerializerTest {

    @Test
    @DisplayName("round-trips a repeating session without a class name in the payload")
    void serialize_roundTripsSessionWithoutClassName() {
        UUID wordId = UUID.randomUUID();
        WordForRepeat word = new WordForRepeat("apple", "яблоко", "an apple", 7L, wordId, true, 2);
        UserState state = new UserState(7L);
        state.setUserStatus(UserStatus.PLANNED_REPEATING_GAME);
        state.setRepeatingState(new RepeatingState(
                new ArrayList<>(List.of(word)), word, new ArrayList<>(List.of("яблоко")), true, null));

        Jackson2JsonRedisSerializer<UserState> serializer = UserStateRedisSerializer.create();
        byte[] payload = serializer.serialize(state);
        assertThat(new String(payload, StandardCharsets.UTF_8)).doesNotContain("@class");

        UserState restored = serializer.deserialize(payload);
        WordForRepeat restoredWord = restored.getRepeatingState().getCurrentRepeatingWord();
        assertThat(restored.getUserId()).isEqualTo(7L);
        assertThat(restored.getUserStatus()).isEqualTo(UserStatus.PLANNED_REPEATING_GAME);
        assertThat(restoredWord.getId()).isEqualTo(wordId);
        assertThat(restoredWord.getOriginal()).isEqualTo("apple");
        assertThat(restoredWord.getTranslation()).isEqualTo("яблоко");
        assertThat(restoredWord.isRepeatFailed()).isTrue();
        assertThat(restoredWord.getCountOfAttempts()).isEqualTo(2);
        assertThat(restored.getRepeatingState().isIncreaseRepeatingCount()).isTrue();
    }

    @Test
    @DisplayName("ignores an embedded class name instead of instantiating it")
    void deserialize_ignoresEmbeddedClassName() {
        byte[] payload = """
                {"@class":"java.lang.Runtime","userId":7,"userStatus":"NO_ACTIVITY"}
                """.getBytes(StandardCharsets.UTF_8);

        UserState state = UserStateRedisSerializer.create().deserialize(payload);

        assertThat(state).isInstanceOf(UserState.class);
        assertThat(state.getUserId()).isEqualTo(7L);
        assertThat(state.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
    }
}
