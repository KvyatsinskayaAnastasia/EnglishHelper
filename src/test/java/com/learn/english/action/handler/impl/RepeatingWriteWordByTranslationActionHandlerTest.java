package com.learn.english.action.handler.impl;

import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_WRITE_WORD_BY_TRANSLATION;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.sentMessages;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.textUpdate;
import static com.learn.english.helper.HandlerTestHelper.word;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepeatingWriteWordByTranslationActionHandlerTest {

    private static final UUID APPLE_ID = UUID.randomUUID();
    private static final UUID DOG_ID = UUID.randomUUID();

    @Mock
    private WordService wordService;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private Random random;

    private RepeatingWriteWordByTranslationActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RepeatingWriteWordByTranslationActionHandler(wordService, telegramHelperService, random);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles writing the word for a translation")
    void getBotAction_returnsWriteWordByTranslation() {
        assertThat(handler.getBotAction()).isEqualTo(REPEATING_WRITE_WORD_BY_TRANSLATION);
    }

    @Test
    @DisplayName("asks to type the word and offers only exit")
    void processAction_newGame_asksToTypeWord() {
        WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
        UserState userState = openGame(List.of(apple));

        handler.processAction(userState, textUpdate(REPEATING_WRITE_WORD_BY_TRANSLATION.getCode()));

        RepeatingState repeatingState = userState.getRepeatingState();
        assertThat(repeatingState.getChosenRepeatingGame()).isEqualTo(REPEATING_WRITE_WORD_BY_TRANSLATION);
        assertThat(repeatingState.getCurrentRepeatingWord()).isSameAs(apple);
        assertThat(repeatingState.getRepeatingLabels()).isEmpty();
        SendMessage message = onlyMessage(telegramHelperService);
        assertThat(message.getText()).isEqualTo("Введите слово, которое переводится как: яблоко");
        assertThat(callbackData(message)).containsExactly(EXIT.getCode());
        verify(wordService, never()).updateRepeatedWord(any(), anyBoolean());
        verify(wordService, never()).getRandomTranslationsByUserId(any(), any(), any());
        verify(wordService, never()).getRandomOriginalsByUserId(any(), any(), any());
    }

    @Test
    @DisplayName("grows the hint by one letter after each wrong answer and stops at the whole word")
    void processAction_wrongAnswers_growHintUpToWordLength() {
        WordForRepeat card = word(APPLE_ID, "to", "к");
        UserState userState = playing(card, List.of(card));

        handler.processAction(userState, textUpdate("no"));
        handler.processAction(userState, textUpdate("nope"));
        handler.processAction(userState, textUpdate("zzz"));

        assertThat(card.isRepeatFailed()).isTrue();
        assertThat(card.getCountOfAttempts()).isEqualTo(3);
        assertThat(userState.getRepeatingState().getRepeatingWords()).containsExactly(card);
        assertThat(sentMessages(telegramHelperService)).extracting(SendMessage::getText).containsExactly(
                hint("no", "к", "t"),
                hint("nope", "к", "to"),
                hint("zzz", "к", "to"));
        verify(wordService, never()).updateRepeatedWord(any(), anyBoolean());
    }

    @Test
    @DisplayName("accepts a different case, then asks for the next word")
    void processAction_correctAnswer_movesToNextWord() {
        WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
        WordForRepeat dog = word(DOG_ID, "dog", "собака");
        UserState userState = playing(apple, List.of(apple, dog));

        handler.processAction(userState, textUpdate("APPLE"));

        assertThat(userState.getRepeatingState().getRepeatingWords()).containsExactly(dog);
        assertThat(userState.getRepeatingState().getCurrentRepeatingWord()).isSameAs(dog);
        assertThat(userState.getRepeatingState().getRepeatingLabels()).isEmpty();
        verify(wordService).updateRepeatedWord(APPLE_ID, false);
        List<SendMessage> messages = sentMessages(telegramHelperService);
        assertThat(messages).extracting(SendMessage::getText).containsExactly(
                "Правильно!\nСлово: apple\nПеревод: яблоко",
                "Введите слово, которое переводится как: собака");
        assertThat(callbackData(messages.get(1))).containsExactly(EXIT.getCode());
    }

    @Test
    @DisplayName("records a failed attempt and finishes after the corrected answer")
    void processAction_wrongThenCorrectOnLastWord_finishes() {
        WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
        UserState userState = playing(apple, List.of(apple));
        when(wordService.countWordsForRepeat(USER_ID)).thenReturn(0);

        handler.processAction(userState, textUpdate("aple"));
        handler.processAction(userState, textUpdate("apple"));

        assertThat(apple.isRepeatFailed()).isTrue();
        assertThat(apple.getCountOfAttempts()).isEqualTo(1);
        verify(wordService).updateRepeatedWord(APPLE_ID, true);
        assertThat(userState.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
        assertThat(userState.getRepeatingState()).isNull();
        assertThat(sentMessages(telegramHelperService)).extracting(SendMessage::getText).containsExactly(
                hint("aple", "яблоко", "a"),
                "Правильно!\nСлово: apple\nПеревод: яблоко",
                "Все слова повторены!");
    }

    private static String hint(String answer, String translation, String prefix) {
        return "Не правильный ответ: " + answer + "\nПопробуйте еще!\nВведите слово, которое переводится как: "
                + translation + ".\nподсказка: " + prefix;
    }

    private static UserState openGame(List<WordForRepeat> words) {
        UserState userState = new UserState(USER_ID);
        userState.setRepeatingState(new RepeatingState(new ArrayList<>(words), null, null, false, null));
        return userState;
    }

    private static UserState playing(WordForRepeat current, List<WordForRepeat> queue) {
        UserState userState = new UserState(USER_ID);
        userState.setRepeatingState(new RepeatingState(
                new ArrayList<>(queue), current, new ArrayList<>(), false, REPEATING_WRITE_WORD_BY_TRANSLATION));
        return userState;
    }
}
