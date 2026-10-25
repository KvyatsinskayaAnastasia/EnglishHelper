package com.learn.english.service;

import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.impl.BaseTelegramService;
import com.learn.english.user.handler.UserStatusHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramServiceTest {

    private static final Long USER_ID = 99L;
    private static final Integer LAST_MESSAGE_ID = 555;

    @Mock
    private UserStatusHandler noActivityHandler;

    @Mock
    private TelegramHelperService telegramHelperService;

    @Mock
    private UserStateService userStateService;

    @Mock
    private WordService wordService;

    private BaseTelegramService service;

    @BeforeEach
    void setUp() {
        Map<UserStatus, UserStatusHandler> handlers = new EnumMap<>(UserStatus.class);
        handlers.put(UserStatus.NO_ACTIVITY, noActivityHandler);
        service = new BaseTelegramService(handlers, telegramHelperService, userStateService, wordService);
    }

    @Nested
    @DisplayName("processScheduledRepeat")
    class ProcessScheduledRepeatTests {

        @Test
        @DisplayName("sends a reminder with the number of words due for the user")
        void processScheduledRepeat_sendsCountMessage() {
            UserState userState = new UserState(USER_ID);
            when(userStateService.getUserState(USER_ID)).thenReturn(userState);
            when(wordService.countWordsForRepeat(USER_ID)).thenReturn(4);

            service.processScheduledRepeat(USER_ID);

            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramHelperService).sendMessage(captor.capture());
            SendMessage sent = captor.getValue();
            assertThat(sent.getChatId()).isEqualTo(USER_ID.toString());
            assertThat(sent.getText()).isEqualTo("У вас 4 неизученных слов. Вперед повторять!");
        }
    }

    @Nested
    @DisplayName("processUpdate")
    class ProcessUpdateTests {

        @Test
        @DisplayName("routes a text message to the handler of the current status")
        void processUpdate_message_routesToHandler() {
            UserState userState = new UserState(USER_ID);
            when(userStateService.getUserState(USER_ID)).thenReturn(userState);
            Update update = messageUpdate();

            service.processUpdate(update);

            verify(noActivityHandler).processAction(userState, update);
            verify(telegramHelperService, never()).removeKeyboard(any(), any());
        }

        @Test
        @DisplayName("reads userId from callback query")
        void processUpdate_callback_usesCallbackUserId() {
            UserState userState = new UserState(USER_ID);
            when(userStateService.getUserState(USER_ID)).thenReturn(userState);
            Update update = callbackUpdate();

            service.processUpdate(update);

            verify(userStateService).getUserState(USER_ID);
            verify(noActivityHandler).processAction(userState, update);
        }

        @Test
        @DisplayName("clears previous inline keyboard before handling the update")
        void processUpdate_hasLastBotMessage_removesKeyboardAndSaves() {
            UserState userState = new UserState(USER_ID);
            userState.setLastBotCommandMessageId(LAST_MESSAGE_ID);
            when(userStateService.getUserState(USER_ID)).thenReturn(userState);
            Update update = messageUpdate();

            service.processUpdate(update);

            verify(telegramHelperService).removeKeyboard(USER_ID, LAST_MESSAGE_ID);
            assertThat(userState.getLastBotCommandMessageId()).isNull();
            verify(userStateService).saveUserState(userState);
            verify(noActivityHandler).processAction(eq(userState), eq(update));
        }
    }

    private static Update messageUpdate() {
        Update update = new Update();
        Message message = new Message();
        message.setFrom(new User(USER_ID, "Ann", false));
        update.setMessage(message);
        return update;
    }

    private static Update callbackUpdate() {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setFrom(new User(USER_ID, "Ann", false));
        update.setCallbackQuery(callbackQuery);
        return update;
    }
}
