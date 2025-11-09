package com.learn.english.utils;

import com.learn.english.model.BotAction;
import org.apache.commons.collections4.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ButtonUtils {
    public static final int DEFAULT_NUMBER_ROW_SIZE = 5;

    public static SendMessage sendWithNumberButtons(Long userId, String message, List<String> buttonLabels,
                                                    List<BotAction> botActions, int actionRowSize) {
        String fullText = createNumberedText(message, buttonLabels);
        InlineKeyboardMarkup keyboard = createCombinedKeyboard(buttonLabels.size(), botActions, actionRowSize);
        return createSendMessage(userId, fullText, keyboard);
    }

    public static SendMessage sendWithActionButtons(Long userId, String text, List<BotAction> botActions, int rowSize) {
        InlineKeyboardMarkup keyboard = createCombinedKeyboard(0, botActions, rowSize);
        return createSendMessage(userId, text, keyboard);
    }

    private static String createNumberedText(String message, List<String> buttonLabels) {
        if (CollectionUtils.isEmpty(buttonLabels)) return message;

        StringBuilder sb = new StringBuilder(message).append("\n");
        IntStream.range(0, buttonLabels.size())
                .forEach(i -> sb.append(i + 1).append(". ").append(buttonLabels.get(i))
                        .append(i < buttonLabels.size() - 1 ? "\n" : ""));
        return sb.toString();
    }

    private static InlineKeyboardMarkup createCombinedKeyboard(int buttonsCount, List<BotAction> botActions, int actionRowSize) {
        List<InlineKeyboardRow> rows = createNumberRows(buttonsCount);
        rows.addAll(createActionRows(botActions, actionRowSize));
        return new InlineKeyboardMarkup(rows);
    }

    private static List<InlineKeyboardRow> createNumberRows(int buttonsCount) {
        return createRows(buttonsCount, i -> createNumberButton(String.valueOf(i + 1)), DEFAULT_NUMBER_ROW_SIZE);
    }

    private static List<InlineKeyboardRow> createActionRows(List<BotAction> botActions, int rowSize) {
        return createRows(botActions.size(), i -> createActionButton(botActions.get(i)), rowSize);
    }

    private static List<InlineKeyboardRow> createRows(int count, Function<Integer, InlineKeyboardButton> buttonCreator,
                                                      int rowSize) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();

        for (int i = 0; i < count; i++) {
            currentRow.add(buttonCreator.apply(i));
            if ((i + 1) % rowSize == 0 || i == count - 1) {
                rows.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }
        return rows;
    }

    private static InlineKeyboardButton createNumberButton(String number) {
        return createButton(number, number);
    }

    private static InlineKeyboardButton createActionButton(BotAction action) {
        return createButton(action.getMessage(), action.getCode());
    }

    private static InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private static SendMessage createSendMessage(long userId, String text, InlineKeyboardMarkup keyboard) {
        return SendMessage.builder()
                .chatId(userId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
    }
}