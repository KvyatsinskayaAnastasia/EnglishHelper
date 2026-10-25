package com.learn.english.service;

import com.learn.english.exception.TelegramOperationException;
import com.learn.english.service.impl.BaseTelegramHelperService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramHelperServiceTest {

    private static final Long CHAT_ID = 12345L;
    private static final Integer MESSAGE_ID = 678;
    private static final String MESSAGE_TEXT = "test";

    @Mock
    private TelegramClient telegramClient;

    @InjectMocks
    private BaseTelegramHelperService service;

    @Nested
    @DisplayName("sendMessage")
    class SendMessageTests {

        @Test
        @DisplayName("returns messageId on success and passes the message to the client")
        void sendMessage_success_returnsMessageId() throws TelegramApiException {
            SendMessage request = SendMessage.builder()
                    .chatId(CHAT_ID)
                    .text(MESSAGE_TEXT)
                    .build();
            Message response = new Message();
            response.setMessageId(MESSAGE_ID);
            when(telegramClient.execute(any(SendMessage.class))).thenReturn(response);

            Integer result = service.sendMessage(request);

            assertThat(result).isEqualTo(MESSAGE_ID);
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramClient).execute(captor.capture());
            assertThat(captor.getValue().getChatId()).isEqualTo(CHAT_ID.toString());
            assertThat(captor.getValue().getText()).isEqualTo(MESSAGE_TEXT);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null message and never calls the client")
        void sendMessage_nullMessage_throwsIllegalArgument() {
            assertThatThrownBy(() -> service.sendMessage(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");

            verifyNoInteractions(telegramClient);
        }

        @Test
        @DisplayName("wraps TelegramApiException into RuntimeException (current behavior)")
        void sendMessage_telegramError_throwsTelegramOperationException() throws TelegramApiException {
            SendMessage request = SendMessage.builder()
                    .chatId(CHAT_ID)
                    .text(MESSAGE_TEXT)
                    .build();
            TelegramApiException cause = new TelegramApiException("something wrong");
            when(telegramClient.execute(any(SendMessage.class))).thenThrow(cause);

            assertThatThrownBy(() -> service.sendMessage(request))
                    .isInstanceOf(TelegramOperationException.class)
                    .hasCause(cause)
                    .hasMessageContaining("sendMessage");
        }
    }

    @Nested
    @DisplayName("removeKeyboard")
    class RemoveKeyboardTests {

        @Test
        @DisplayName("executes EditMessageReplyMarkup with chatId, messageId and null markup")
        void removeKeyboard_success_executesEditWithNullMarkup() throws TelegramApiException {
            service.removeKeyboard(CHAT_ID, MESSAGE_ID);

            ArgumentCaptor<EditMessageReplyMarkup> captor =
                    ArgumentCaptor.forClass(EditMessageReplyMarkup.class);
            verify(telegramClient).execute(captor.capture());

            EditMessageReplyMarkup sent = captor.getValue();
            assertThat(sent.getChatId()).isEqualTo(CHAT_ID.toString());
            assertThat(sent.getMessageId()).isEqualTo(MESSAGE_ID);
            assertThat(sent.getReplyMarkup()).isNull();
        }

        @Test
        @DisplayName("skips API call and does nothing when chatId is null")
        void removeKeyboard_nullChatId_noInteraction() {
            service.removeKeyboard(null, MESSAGE_ID);

            verifyNoInteractions(telegramClient);
        }

        @Test
        @DisplayName("skips API call and does nothing when messageId is null")
        void removeKeyboard_nullMessageId_noInteraction() {
            service.removeKeyboard(CHAT_ID, null);

            verifyNoInteractions(telegramClient);
        }

        @Test
        @DisplayName("wraps TelegramApiException into TelegramOperationException with context")
        void removeKeyboard_telegramError_throwsTelegramOperationException() throws TelegramApiException {
            TelegramApiException cause = new TelegramApiException("something wrong");
            when(telegramClient.execute(any(EditMessageReplyMarkup.class))).thenThrow(cause);

            assertThatThrownBy(() -> service.removeKeyboard(CHAT_ID, MESSAGE_ID))
                    .isInstanceOf(TelegramOperationException.class)
                    .hasCause(cause)
                    .hasMessageContaining("removeKeyboard");
        }
    }
}
