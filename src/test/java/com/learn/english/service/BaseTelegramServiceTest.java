package com.learn.english.service;

import com.learn.english.handler.BotActionHandler;
import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.service.impl.BaseTelegramService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseTelegramServiceTest {

    @Mock
    private Map<BotAction, BotActionHandler> botActionHandlers;

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private UserStateService userStateService;

    @InjectMocks
    private BaseTelegramService telegramService;

    @Mock
    private BotActionHandler mockHandler;

    @Test
    void processScheduledRepeat_ShouldProcessCorrectly() throws TelegramApiException {
        // Arrange
        Long userId = 456L;
        UserState userState = new UserState(userId);
        SendMessage expectedMessage = SendMessage.builder().chatId(userId.toString()).text("Test message").build();

        when(userStateService.getUserState(userId)).thenReturn(userState);
        when(botActionHandlers.get(BotAction.SCHEDULED_REPEAT)).thenReturn(mockHandler);
        when(mockHandler.processAction(userState, null)).thenReturn(expectedMessage);
        doNothing().when(userStateService).saveUserState(userState);

        // Act
        telegramService.processScheduledRepeat(userId);

        // Assert
        verify(userStateService).getUserState(userId);
        verify(botActionHandlers).get(BotAction.SCHEDULED_REPEAT);
        verify(mockHandler).processAction(userState, null);
        verify(userStateService).saveUserState(userState);
        verify(telegramClient).execute(expectedMessage);
    }

    @Test
    void processUpdate_WithTextMessage_ShouldProcessCorrectly() throws TelegramApiException {
        // Arrange
        Long userId = 456L;
        String text = "test message";
        Update update = createTextUpdate(userId, text);

        UserState userState = new UserState(userId);
        userState.setUserStatus(UserStatus.FILLING_WORD);

        SendMessage expectedMessage = SendMessage.builder().chatId(userId.toString()).text("Response").build();

        when(userStateService.getUserState(userId)).thenReturn(userState);
        when(botActionHandlers.getOrDefault(any(BotAction.class), any())).thenReturn(mockHandler);
        when(mockHandler.processAction(any(UserState.class), anyString())).thenReturn(expectedMessage);
        doNothing().when(userStateService).saveUserState(any(UserState.class));

        // Act
        telegramService.processUpdate(update);

        // Assert
        verify(userStateService).getUserState(userId);
        verify(botActionHandlers).getOrDefault(BotAction.ADD_WORD_WITH_BOT_REQUEST_TRANSLATION, null);
        verify(mockHandler).processAction(userState, text);
        verify(userStateService).saveUserState(userState);
        verify(telegramClient).execute(expectedMessage);
    }

    @Test
    void processUpdate_WithCallbackQuery_ShouldProcessCorrectly() throws TelegramApiException {
        // Arrange
        Long userId = 456L;
        String callbackData = "EXIT";
        Update update = createCallbackUpdate(userId, callbackData);

        UserState userState = new UserState(userId);
        userState.setUserStatus(UserStatus.FILLING_WORD);

        SendMessage expectedMessage = SendMessage.builder().chatId(userId.toString()).text("Callback response").build();

        when(userStateService.getUserState(userId)).thenReturn(userState);
        when(botActionHandlers.getOrDefault(any(BotAction.class), any())).thenReturn(mockHandler);
        when(mockHandler.processAction(any(UserState.class), anyString())).thenReturn(expectedMessage);
        doNothing().when(userStateService).saveUserState(any(UserState.class));

        // Act
        telegramService.processUpdate(update);

        // Assert
        verify(userStateService).getUserState(userId);
        verify(botActionHandlers).getOrDefault(BotAction.EXIT, null);
        verify(mockHandler).processAction(userState, callbackData);
        verify(userStateService).saveUserState(userState);
        verify(telegramClient).execute(expectedMessage);
    }

    @Test
    void processUpdate_WithLongProcessingAction_ShouldSendWaitingMessage() throws TelegramApiException {
        // Arrange
        Long userId = 456L;
        String text = "test";
        Update update = createTextUpdate(userId, text);

        UserState userState = new UserState(userId);
        userState.setUserStatus(UserStatus.FILLING_WORD);

        SendMessage expectedMessage = SendMessage.builder().chatId(userId.toString()).text("Response").build();

        when(userStateService.getUserState(userId)).thenReturn(userState);
        when(botActionHandlers.getOrDefault(any(BotAction.class), any())).thenReturn(mockHandler);
        when(mockHandler.processAction(any(UserState.class), anyString())).thenReturn(expectedMessage);
        doNothing().when(userStateService).saveUserState(any(UserState.class));

        // Act
        telegramService.processUpdate(update);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, times(2)).execute(messageCaptor.capture());

        List<SendMessage> sentMessages = messageCaptor.getAllValues();
        assertEquals("Бот жив, но долго думает. Терпение...", sentMessages.get(0).getText());
        assertEquals("Response", sentMessages.get(1).getText());
    }

    @Test
    void processUpdate_WithInvalidCallback_ShouldSendErrorMessage() throws TelegramApiException {
        // Arrange
        Long userId = 456L;
        String invalidCallback = "ADD_WORD_SAVE_TRANSLATION_REQUEST_EXAMPLE";
        Update update = createCallbackUpdate(userId, invalidCallback);

        UserState userState = new UserState(userId);
        userState.setUserStatus(UserStatus.REPEATING_CHOOSE_ORIGINAL);

        SendMessage helpMessage = SendMessage.builder().chatId(userId.toString()).text("Response").build();
        when(botActionHandlers.get(any(BotAction.class))).thenReturn(mockHandler);
        when(botActionHandlers.getOrDefault(any(), any())).thenReturn(mockHandler);
        when(mockHandler.processAction(any(UserState.class), anyString())).thenReturn(helpMessage);
        when(userStateService.getUserState(userId)).thenReturn(userState);

        // Act
        telegramService.processUpdate(update);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, times(2)).execute(messageCaptor.capture());

        List<SendMessage> sentMessages = messageCaptor.getAllValues();
        assertEquals("Что-то не то нажато =.=", sentMessages.get(0).getText());
        assertEquals("Response", sentMessages.get(1).getText());
        verifyNoMoreInteractions(botActionHandlers);
    }

    @Test
    void processUpdate_WithTelegramApiException_ShouldThrowRuntimeException() throws TelegramApiException {
        // Arrange
        Long userId = 456L;
        String text = "test";
        Update update = createTextUpdate(userId, text);

        UserState userState = new UserState(userId);
        userState.setUserStatus(UserStatus.FILLING_WORD);

        when(userStateService.getUserState(userId)).thenReturn(userState);
        doThrow(new TelegramApiException("API error")).when(telegramClient).execute(any(SendMessage.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> telegramService.processUpdate(update));
    }

    @Test
    void processUpdate_WithUpdateWithoutText_ShouldNotProcess() {
        // Arrange
        Update update = new Update();
        Message message = new Message();
        message.setFrom(new User(456L, "test", false));
        message.setChat(new Chat(123L, "private"));
        // No text set
        update.setMessage(message);

        // Act
        telegramService.processUpdate(update);

        // Assert
        verifyNoInteractions(userStateService, botActionHandlers, telegramClient);
    }

    private Update createTextUpdate(Long userId, String text) {
        Update update = new Update();
        Message message = new Message();
        User from = User.builder().id(userId).firstName("test").isBot(false).build();
        message.setFrom(from);
        message.setChat(new Chat(userId, "private"));
        message.setText(text);
        update.setMessage(message);
        return update;
    }

    private Update createCallbackUpdate(Long userId, String callbackData) {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        User from = User.builder().id(userId).firstName("test").isBot(false).build();
        callbackQuery.setFrom(from);

        Message message = new Message();
        message.setChat(new Chat(userId, "private"));
        callbackQuery.setMessage(message);
        callbackQuery.setData(callbackData);

        update.setCallbackQuery(callbackQuery);
        return update;
    }
}