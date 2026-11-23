package com.learn.english.action.handler.impl;

import com.learn.english.entity.WordEO;
import com.learn.english.mapper.WordMapper;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.repository.WordRepository;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.WordArchiveService;
import com.learn.english.service.impl.BaseWordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.callbackUpdate;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.word;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_TRANSLATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepeatingAnswerIntervalTest {

    private static final UUID WORD_ID = UUID.randomUUID();

    @Mock
    private WordRepository wordRepository;

    @Mock
    private WordMapper wordMapper;

    @Mock
    private WordArchiveService wordArchiveService;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private Random random;

    private RepeatingChooseTranslationActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RepeatingChooseTranslationActionHandler(
                new BaseWordService(wordRepository, wordMapper, wordArchiveService),
                telegramHelperService,
                random);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("correct answer in random repeating does not change repeat_at")
    void correctAnswer_random_leavesRepeatAt() {
        LocalDateTime repeatAt = LocalDateTime.of(2026, 10, 26, 9, 0);
        WordEO card = storedWord(2, repeatAt);
        lenient().when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(card));

        handler.processAction(playing(false), callbackUpdate("1"));

        assertThat(card.getRepeatAt()).isEqualTo(repeatAt);
        assertThat(card.getRepeatedCount()).isEqualTo(2);
        verify(wordRepository, never()).save(any());
    }

    @ParameterizedTest(name = "repeated_count {0} schedules the next repeat in {1} days")
    @CsvSource({
            "0, 1",
            "1, 2",
            "2, 3",
            "3, 4",
            "4, 7",
            "5, 14",
            "6, 21"
    })
    @DisplayName("correct answer in planned repeating schedules repeat_at by the interval table")
    void correctAnswer_planned_schedulesByIntervalTable(int repeatedCount, int delayDays) {
        WordEO card = storedWord(repeatedCount, LocalDateTime.of(2026, 10, 1, 8, 0));
        when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(card));

        LocalDateTime before = LocalDateTime.now();
        handler.processAction(playing(true), callbackUpdate("1"));

        assertThat(card.getRepeatedCount()).isEqualTo(repeatedCount + 1);
        assertThat(card.getRepeatAt()).isCloseTo(before.plusDays(delayDays), within(2, ChronoUnit.SECONDS));
        verify(wordRepository).save(card);
    }

    @Test
    @DisplayName("correct answer after a failed attempt removes the card restored from Redis and shifts the interval once")
    void correctAnswer_afterFailedAttemptRestoredFromRedis_removesCardOnce() {
        WordEO card = storedWord(2, LocalDateTime.of(2026, 10, 1, 8, 0));
        when(wordRepository.findById(WORD_ID)).thenReturn(Optional.of(card));

        WordForRepeat queued = word(WORD_ID, "apple", "яблоко");
        WordForRepeat current = word(WORD_ID, "apple", "яблоко");
        current.setRepeatFailed(true);

        UserState userState = new UserState(USER_ID);
        userState.setUserStatus(UserStatus.PLANNED_REPEATING_GAME);
        userState.setRepeatingState(new RepeatingState(
                new ArrayList<>(List.of(queued)),
                current,
                new ArrayList<>(List.of("яблоко")),
                true,
                REPEATING_CHOOSE_TRANSLATION));

        UserState restored = redisRoundTrip(userState);
        WordForRepeat restoredCurrent = restored.getRepeatingState().getCurrentRepeatingWord();
        WordForRepeat restoredQueued = restored.getRepeatingState().getRepeatingWords().get(0);
        assertThat(restoredCurrent).isSameAs(restoredQueued);
        assertThat(restoredCurrent.isRepeatFailed()).isTrue();

        handler.processAction(restored, callbackUpdate("1"));

        assertThat(restored.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
        assertThat(restored.getRepeatingState()).isNull();
        assertThat(card.getRepeatedCount()).isEqualTo(1);
        verify(wordRepository, times(1)).save(card);
    }

    @Test
    @DisplayName("reads a saved session that still contains the removed attempt counter")
    void redisRoundTrip_legacyAttemptCount_keepsFailureFlag() {
        WordForRepeat queued = word(WORD_ID, "apple", "яблоко");
        WordForRepeat current = word(WORD_ID, "apple", "яблоко");
        current.setRepeatFailed(true);
        UserState userState = new UserState(USER_ID);
        userState.setRepeatingState(new RepeatingState(
                new ArrayList<>(List.of(queued)),
                current,
                new ArrayList<>(List.of("яблоко")),
                true,
                REPEATING_CHOOSE_TRANSLATION));

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        String json = new String(serializer.serialize(userState), StandardCharsets.UTF_8);
        String legacy = json.replace("\"repeatFailed\":true", "\"repeatFailed\":true,\"countOfAttempts\":4");

        UserState restored = serializer.deserialize(legacy.getBytes(StandardCharsets.UTF_8), UserState.class);

        WordForRepeat restoredCurrent = restored.getRepeatingState().getCurrentRepeatingWord();
        assertThat(restoredCurrent.isRepeatFailed()).isTrue();
        assertThat(restoredCurrent).isSameAs(restored.getRepeatingState().getRepeatingWords().get(0));
    }

    private static UserState redisRoundTrip(UserState userState) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        return serializer.deserialize(serializer.serialize(userState), UserState.class);
    }

    private static WordEO storedWord(int repeatedCount, LocalDateTime repeatAt) {
        WordEO card = new WordEO();
        card.setId(WORD_ID);
        card.setUserId(USER_ID);
        card.setOriginal("apple");
        card.setTranslation("яблоко");
        card.setRepeatedCount(repeatedCount);
        card.setRepeatAt(repeatAt);
        return card;
    }

    private static UserState playing(boolean increaseRepeatingCount) {
        WordForRepeat apple = word(WORD_ID, "apple", "яблоко");
        UserState userState = new UserState(USER_ID);
        userState.setRepeatingState(new RepeatingState(
                new ArrayList<>(List.of(apple)),
                apple,
                new ArrayList<>(List.of("яблоко")),
                increaseRepeatingCount,
                REPEATING_CHOOSE_TRANSLATION));
        return userState;
    }
}
