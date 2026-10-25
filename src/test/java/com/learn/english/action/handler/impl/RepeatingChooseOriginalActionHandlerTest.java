package com.learn.english.action.handler.impl;

import com.learn.english.model.Icon;
import com.learn.english.model.RepeatingState;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.model.WordForRepeat;
import com.learn.english.service.TelegramHelperService;
import com.learn.english.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.learn.english.model.BotAction.ADD_WORD_REQUEST_ORIGINAL;
import static com.learn.english.model.BotAction.EXIT;
import static com.learn.english.model.BotAction.REPEATING_CHOOSE_ORIGINAL;
import static com.learn.english.model.BotAction.REPEATING_PLANNED;
import static com.learn.english.model.BotAction.REPEATING_RANDOM;
import static com.learn.english.helper.HandlerTestHelper.MESSAGE_ID;
import static com.learn.english.helper.HandlerTestHelper.USER_ID;
import static com.learn.english.helper.HandlerTestHelper.assertButtonRows;
import static com.learn.english.helper.HandlerTestHelper.callbackData;
import static com.learn.english.helper.HandlerTestHelper.callbackUpdate;
import static com.learn.english.helper.HandlerTestHelper.onlyMessage;
import static com.learn.english.helper.HandlerTestHelper.sentMessages;
import static com.learn.english.helper.HandlerTestHelper.stubSend;
import static com.learn.english.helper.HandlerTestHelper.textUpdate;
import static com.learn.english.helper.HandlerTestHelper.word;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepeatingChooseOriginalActionHandlerTest {

    private static final UUID APPLE_ID = UUID.randomUUID();
    private static final UUID DOG_ID = UUID.randomUUID();

    @Mock
    private WordService wordService;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private Random random;

    private RepeatingChooseOriginalActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RepeatingChooseOriginalActionHandler(wordService, telegramHelperService, random);
        stubSend(telegramHelperService);
    }

    @Test
    @DisplayName("handles choosing the original word")
    void getBotAction_returnsChooseOriginal() {
        assertThat(handler.getBotAction()).isEqualTo(REPEATING_CHOOSE_ORIGINAL);
    }

    @Test
    @DisplayName("fails when the repeating session is missing")
    void processAction_noSession_throws() {
        assertThatThrownBy(() -> handler.processAction(new UserState(USER_ID), textUpdate("x")))
                .isInstanceOf(NullPointerException.class);
    }

    @Nested
    @DisplayName("starting a round")
    class StartTests {

        @Test
        @DisplayName("picks a word, sorts the original options and asks to choose")
        void processAction_newGame_asksForOriginal() {
            WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
            WordForRepeat dog = word(DOG_ID, "dog", "собака");
            UserState userState = openGame(List.of(apple, dog));
            when(random.nextInt(2)).thenReturn(1);
            when(wordService.getRandomOriginalsByUserId(USER_ID, DOG_ID, 3)).thenReturn(List.of("pear", "banana"));

            handler.processAction(userState, textUpdate(REPEATING_CHOOSE_ORIGINAL.getCode()));

            RepeatingState repeatingState = userState.getRepeatingState();
            assertThat(repeatingState.getChosenRepeatingGame()).isEqualTo(REPEATING_CHOOSE_ORIGINAL);
            assertThat(repeatingState.getCurrentRepeatingWord()).isSameAs(dog);
            assertThat(repeatingState.getRepeatingLabels()).containsExactly("banana", "dog", "pear");
            SendMessage message = onlyMessage(telegramHelperService);
            assertThat(message.getText()).isEqualTo(
                    "Выберите слово, которое переводится как: собака\n\n1. banana\n2. dog\n3. pear");
            assertThat(callbackData(message)).containsExactly("1", "2", "3", EXIT.getCode());
            assertButtonRows(message, 3, 1);
            assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
            verify(wordService, never()).updateRepeatedWord(any(), anyBoolean());
        }

        @Test
        @DisplayName("asks about the only original when there are no distractors")
        void processAction_noDistractors_offersCorrectOriginalOnly() {
            WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
            UserState userState = openGame(List.of(apple));
            when(wordService.getRandomOriginalsByUserId(USER_ID, APPLE_ID, 3)).thenReturn(List.of());

            handler.processAction(userState, textUpdate("start"));

            assertThat(userState.getRepeatingState().getRepeatingLabels()).containsExactly("apple");
            assertThat(onlyMessage(telegramHelperService).getText()).isEqualTo(
                    "Выберите слово, которое переводится как: яблоко\n\n1. apple");
        }

        @Test
        @DisplayName("finishes immediately when the session has no words left")
        void processAction_emptyQueue_finishes() {
            UserState userState = openGame(List.of());
            when(wordService.countWordsForRepeat(USER_ID)).thenReturn(0);

            handler.processAction(userState, textUpdate("start"));

            assertThat(userState.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
            assertThat(userState.getRepeatingState()).isNull();
            SendMessage message = onlyMessage(telegramHelperService);
            assertThat(message.getText()).isEqualTo("Все слова повторены!");
            assertThat(callbackData(message)).containsExactly(
                    ADD_WORD_REQUEST_ORIGINAL.getCode(),
                    REPEATING_PLANNED.getCode(),
                    REPEATING_RANDOM.getCode());
            verify(wordService, never()).getRandomOriginalsByUserId(any(), any(), any());
            verify(wordService, never()).updateRepeatedWord(any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("answering")
    class AnswerTests {

        @Test
        @DisplayName("accepts the numbered option that matches the original and moves on")
        void processAction_correctOption_movesToNextWord() {
            WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
            WordForRepeat dog = word(DOG_ID, "dog", "собака");
            UserState userState = playing(apple, List.of(apple, dog), List.of("ant", "apple", "dog"));
            when(wordService.getRandomOriginalsByUserId(USER_ID, DOG_ID, 3)).thenReturn(List.of("ant"));

            handler.processAction(userState, callbackUpdate("2"));

            assertThat(userState.getRepeatingState().getRepeatingWords()).containsExactly(dog);
            assertThat(userState.getRepeatingState().getCurrentRepeatingWord()).isSameAs(dog);
            assertThat(userState.getRepeatingState().getRepeatingLabels()).containsExactly("ant", "dog");
            verify(wordService).updateRepeatedWord(APPLE_ID, false);
            List<SendMessage> messages = sentMessages(telegramHelperService);
            assertThat(messages).hasSize(2);
            assertThat(messages.get(0).getText()).isEqualTo("Правильно!\nСлово: apple\nПеревод: яблоко");
            assertThat(messages.get(0).getReplyMarkup()).isNull();
            assertThat(messages.get(1).getText()).isEqualTo(
                    "Выберите слово, которое переводится как: собака\n\n1. ant\n2. dog");
        }

        @Test
        @DisplayName("accepts the original typed in a different case")
        void processAction_correctTextIgnoringCase_accepts() {
            WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
            apple.setRepeatFailed(true);
            UserState userState = playing(apple, List.of(apple), List.of("ant", "apple", "dog"));
            when(wordService.countWordsForRepeat(USER_ID)).thenReturn(4);

            handler.processAction(userState, textUpdate("APPLE"));

            verify(wordService).updateRepeatedWord(APPLE_ID, true);
            assertThat(userState.getUserStatus()).isEqualTo(UserStatus.NO_ACTIVITY);
            assertThat(userState.getRepeatingState()).isNull();
            List<SendMessage> messages = sentMessages(telegramHelperService);
            assertThat(messages).extracting(SendMessage::getText).containsExactly(
                    "Правильно!\nСлово: apple\nПеревод: яблоко",
                    "Отлично! Осталось повторить: 4");
            assertThat(callbackData(messages.get(1))).containsExactly(
                    ADD_WORD_REQUEST_ORIGINAL.getCode(),
                    REPEATING_PLANNED.getCode(),
                    REPEATING_RANDOM.getCode());
            assertButtonRows(messages.get(1), 2, 1);
        }

        @Test
        @DisplayName("marks the selected option and asks again when the number is wrong")
        void processAction_wrongOption_marksItAndRetries() {
            WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
            UserState userState = playing(apple, List.of(apple), List.of("ant", "apple", "dog"));

            handler.processAction(userState, callbackUpdate("1"));

            String marked = "ant " + Icon.NOT.get();
            assertThat(userState.getRepeatingState().getRepeatingLabels()).containsExactly(marked, "apple", "dog");
            assertThat(apple.isRepeatFailed()).isTrue();
            assertThat(userState.getRepeatingState().getRepeatingWords()).containsExactly(apple);
            verify(wordService, never()).updateRepeatedWord(any(), anyBoolean());
            SendMessage message = onlyMessage(telegramHelperService);
            assertThat(message.getText()).isEqualTo(
                    "Не правильный вариант: ant.\nПопробуйте еще раз!\nВыберете слово, которое переводится как: яблоко\n\n"
                            + "1. " + marked + "\n2. apple\n3. dog");
            assertThat(userState.getLastBotCommandMessageId()).isEqualTo(MESSAGE_ID);
        }

        @Test
        @DisplayName("marks a wrong answer typed exactly as one of the options")
        void processAction_wrongTextMatchingLabel_marksIt() {
            WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
            UserState userState = playing(apple, List.of(apple), List.of("ant", "apple", "dog"));

            handler.processAction(userState, textUpdate("ant"));

            assertThat(userState.getRepeatingState().getRepeatingLabels().get(0))
                    .isEqualTo("ant " + Icon.NOT.get());
            assertThat(apple.isRepeatFailed()).isTrue();
        }

        @Test
        @DisplayName("keeps the options unchanged when the typed answer is not one of them")
        void processAction_wrongTextNotInLabels_leavesLabels() {
            WordForRepeat apple = word(APPLE_ID, "apple", "яблоко");
            UserState userState = playing(apple, List.of(apple), List.of("ant", "apple", "dog"));

            handler.processAction(userState, textUpdate("nope"));

            assertThat(userState.getRepeatingState().getRepeatingLabels()).containsExactly("ant", "apple", "dog");
            assertThat(onlyMessage(telegramHelperService).getText()).isEqualTo(
                    """
                            Не правильный вариант: nope.
                            Попробуйте еще раз!
                            Выберете слово, которое переводится как: яблоко
                            
                            1. ant
                            2. apple
                            3. dog""");
        }

        @Test
        @DisplayName("asks to pick a listed option when the callback is not a number")
        void processAction_nonNumericCallback_asksToChoose() {
            WordForRepeat apple = word(APPLE_ID, "apple", "cat");
            UserState userState = playing(apple, List.of(apple), List.of("ant", "apple", "dog"));

            handler.processAction(userState, callbackUpdate("abc"));

            assertThat(userState.getRepeatingState().getRepeatingLabels()).containsExactly("ant", "apple", "dog");
            assertThat(apple.isRepeatFailed()).isFalse();
            assertThat(onlyMessage(telegramHelperService).getText()).isEqualTo(
                    """
                            Ответ abc отсутствует. Пожалуйста, выберите вариант из предложенных: \
                            Не правильный вариант: abc.
                            Попробуйте еще раз!
                            Выберете слово, которое переводится как: cat
                            
                            1. ant
                            2. apple
                            3. dog""");
        }

        @Test
        @DisplayName("fails when the option number is outside the list")
        void processAction_optionOutOfRange_throws() {
            WordForRepeat apple = word(APPLE_ID, "apple", "cat");
            UserState userState = playing(apple, List.of(apple), List.of("ant", "apple", "dog"));

            assertThatThrownBy(() -> handler.processAction(userState, callbackUpdate("9")))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            verify(telegramHelperService, never()).sendMessage(any());
        }
    }

    private static UserState openGame(List<WordForRepeat> words) {
        UserState userState = new UserState(USER_ID);
        userState.setRepeatingState(new RepeatingState(new ArrayList<>(words), null, null, true, null));
        return userState;
    }

    private static UserState playing(WordForRepeat current, List<WordForRepeat> queue, List<String> labels) {
        UserState userState = new UserState(USER_ID);
        userState.setRepeatingState(new RepeatingState(
                new ArrayList<>(queue), current, new ArrayList<>(labels), true, REPEATING_CHOOSE_ORIGINAL));
        return userState;
    }
}
