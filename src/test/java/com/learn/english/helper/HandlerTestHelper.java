package com.learn.english.helper;

import com.learn.english.model.WordForRepeat;
import com.learn.english.service.TelegramHelperService;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

public final class HandlerTestHelper {

    public static final Long USER_ID = 42L;
    public static final int MESSAGE_ID = 77;

    private HandlerTestHelper() {
    }

    public static Update textUpdate(String text) {
        Update update = new Update();
        Message message = new Message();
        message.setFrom(new User(USER_ID, "Somebody", false));
        message.setText(text);
        update.setMessage(message);
        return update;
    }

    public static Update callbackUpdate(String data) {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setFrom(new User(USER_ID, "Somebody", false));
        callbackQuery.setData(data);
        update.setCallbackQuery(callbackQuery);
        return update;
    }

    public static Update emptyUpdate() {
        return new Update();
    }

    public static WordForRepeat word(UUID id, String original, String translation) {
        return new WordForRepeat(original, translation, "example of " + original, USER_ID, id, false, 0);
    }

    public static List<String> callbackData(SendMessage message) {
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) message.getReplyMarkup();
        return markup.getKeyboard().stream()
                .flatMap(Collection::stream)
                .map(InlineKeyboardButton::getCallbackData)
                .toList();
    }

    public static void assertButtonRows(SendMessage message, int... rowSizes) {
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) message.getReplyMarkup();
        assertThat(markup.getKeyboard()).hasSize(rowSizes.length);
        for (int i = 0; i < rowSizes.length; i++) {
            assertThat(markup.getKeyboard().get(i)).hasSize(rowSizes[i]);
        }
    }

    public static List<SendMessage> sentMessages(TelegramHelperService service) {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(service, atLeastOnce()).sendMessage(captor.capture());
        List<SendMessage> messages = captor.getAllValues();
        assertThat(messages).allSatisfy(message ->
                assertThat(message.getChatId()).isEqualTo(USER_ID.toString()));
        return messages;
    }

    public static SendMessage onlyMessage(TelegramHelperService service) {
        List<SendMessage> messages = sentMessages(service);
        assertThat(messages).hasSize(1);
        return messages.get(0);
    }

    public static void stubSend(TelegramHelperService service) {
        lenient().when(service.sendMessage(any())).thenReturn(MESSAGE_ID);
    }
}
