package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class Bot extends TelegramLongPollingBot {

    public static final Long Vlad_id = 428602990L;

    @Override
    public String getBotUsername() {
        return "MOEX Checker Training";
    }

    /**
     * Возвращает токен Телеграм Бота.
     * Используется при инициализации Бота автоматически
     *
     * @return токен Телеграм Бота
     */
    @Override
    public String getBotToken() {
        return System.getenv("MOEX_BOT_TOKEN");
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {
            Message msg = update.getMessage();
            if (msg.isCommand()) {
                try {
                    commandHandler(msg);
                } catch (SQLException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else if (msg.isUserMessage()) {
                try {
                    textHandler(msg);
                } catch (SQLException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    execute(ForwardMessage.builder().messageId(msg.getMessageId()).fromChatId(String.valueOf(msg.getChatId())).chatId(String.valueOf(Vlad_id)).build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (update.hasCallbackQuery()) {
            callbackHandler(update.getCallbackQuery());
        }
//        sendText(user_id, msg.getText());
    }

    public synchronized void sendText(Long chat_id, String msg_text) {
        sendMessage(SendMessage.builder().chatId(String.valueOf(chat_id)).text(msg_text).build());
    }

    public synchronized void editMessageReplyMarkup(EditMessageReplyMarkup emrp) {
        try {
            execute(emrp);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void deleteMessage(DeleteMessage dm) {
        try {
            execute(dm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void sendMessage(SendMessage smb) {
        try {
            execute(smb);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void commandHandler(Message msg) throws SQLException, ClassNotFoundException {
        String text = msg.getText();
        if (text.equals("/start")) {
            startHandler(msg);
        } else if (text.contains("/add")) {
            sendMessage(keyboardHandler.addSelectedStocks(msg));
        } else if (text.contains("/test")) {
            sendMessage(keyboardHandler.testSelectedStocks(msg));
        } else if (text.contains("/help")) {
            sendMessage(keyboardHandler.sendHelp(msg));
        } else {
            System.out.printf("is Command else 50 %s\n", text);
        }
    }

    public void callbackHandler(CallbackQuery cbq) {
        String data = cbq.getData();
        String chat_id = cbq.getMessage().getChatId().toString();

        System.out.printf("Callback data:\t%s\n", data);
        if (data.contains("/rm")) {
            editMessageReplyMarkup(
                    keyboardHandler.removeSelectedStock(
                            chat_id, data.split(":")[1], cbq.getMessage().getMessageId()
                    )
            );
        } else if (data.contains("/srm")) {
            deleteMessage(DeleteMessage.builder().chatId(chat_id).messageId(cbq.getMessage().getMessageId()).build());
            sendText(cbq.getMessage().getChatId(), "Завершили изменение Избранного");
        }

        System.out.println(data);
    }

    public void textHandler(Message msg) throws SQLException, ClassNotFoundException {
        String user = msg.getFrom().toString();
        System.out.println(user);
        String chat_id = msg.getChatId().toString();

        if (!msg.hasText()) {
            deleteMessage(DeleteMessage.builder().chatId(chat_id).messageId(msg.getMessageId()).build());
            return;
        }

        String text = msg.getText();
        System.out.println(text);

        switch (text) {
            case "Избранное":
                System.out.println("Selected is checked");
                sendMessage(keyboardHandler.sendSelectedStocks(chat_id));
                break;
            case "Изменить избр.":
                sendMessage(keyboardHandler.changeSelectedStocks(chat_id));
                break;
            case "Добавить избр.":
                sendMessage(keyboardHandler.addSelectStocksHelper(chat_id));
                break;
            case "Ссылка на Мосбиржу":
                sendMessage(keyboardHandler.sendMOEXLink(chat_id));
                break;
            default:
                System.out.printf("Default 100 %s\n", text);
        }
    }

    public void startHandler(Message msg) throws SQLException, ClassNotFoundException {
        Long chat_id = msg.getChatId();
        databaseHandler.addNewUser(databaseHandler.user_table_name, String.valueOf(chat_id));

        {
            DeleteMessage dm = new DeleteMessage();
            dm.setChatId(String.valueOf(chat_id));
            dm.setMessageId(msg.getMessageId());

            try {
                execute(dm);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

        }

        String msg_text = String.format("""
                Здравствуйте, %s!

                Чтобы добавить новую избранную акцию, нажмите 'Добавить избр.'.
                
                Чтобы ввести тестовые значения, введите команду /test. Учтите, что эта команда сотрёт старые значения.
                
                Для получения краткой справки введите команду /help.
                """, msg.getFrom().getFirstName());

        SendMessage sm = SendMessage.builder()
                .chatId(String.valueOf(chat_id))
                .text(msg_text)
                .parseMode("Markdown")
                .build();

        {
            ReplyKeyboardMarkup rkb = new ReplyKeyboardMarkup();
            sm.setReplyMarkup(rkb);

            // see_also: https://core.telegram.org/constructor/replyKeyboardMarkup
            rkb.setSelective(false);    // выводить клавиатуру определенным юзерам или всем
            rkb.setResizeKeyboard(true);    // автоподбор размера в зависимости от количества кнопок
            rkb.setOneTimeKeyboard(false);  // скрывать после использования

            // Заполнитель, который будет отображаться в поле ввода при активной клавиатуре; от 1 до 64 символов.
            rkb.setInputFieldPlaceholder("Выберите одну из команд, пожалуйста");

            List<KeyboardRow> kb = new ArrayList<>();   // создаём список строк клавиатуры

            KeyboardRow fkr = new KeyboardRow();
            fkr.add(new KeyboardButton("Избранное"));
            fkr.add(new KeyboardButton("Изменить избр."));
            fkr.add(new KeyboardButton("Добавить избр."));

            KeyboardRow skr = new KeyboardRow();
            skr.add(new KeyboardButton("Ссылка на Мосбиржу"));

            kb.add(fkr);
            kb.add(skr);

            rkb.setKeyboard(kb);
        }

        sendMessage(sm);
    }
}
