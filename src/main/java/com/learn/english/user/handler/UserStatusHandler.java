package com.learn.english.user.handler;

import com.learn.english.model.BotAction;
import com.learn.english.model.UserState;
import com.learn.english.model.UserStatus;
import com.learn.english.utils.ButtonUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface UserStatusHandler {
    UserStatus getUserStatus();

    void processAction(UserState userState, Update update);

    default SendMessage sendWithNumberButtons(Long userId, String message, List<String> buttonLabels,
                                              List<BotAction> botActions, int actionRowSize) {
        return ButtonUtils.sendWithNumberButtons(userId, message, buttonLabels, botActions, actionRowSize);
    }

    default SendMessage sendWithActionButtons(Long userId, String text, List<BotAction> botActions, int rowSize) {
        return ButtonUtils.sendWithActionButtons(userId, text, botActions, rowSize);
    }

    default BotAction getBotAction(Update update) {
        if (update.getMessage() != null) {
            return BotAction.getOptionFromCode(update.getMessage().getText());
        }
        if (update.getCallbackQuery() != null) {
            return BotAction.getOptionFromCode(update.getCallbackQuery().getData());
        }
        return null;
    }
}
