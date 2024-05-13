package org.example;

import org.h2.jdbc.JdbcSQLTransientException;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;

public class keyboardHandler {
    private static final SendMessage.SendMessageBuilder listOfStocksEmpty = SendMessage.builder().text("Список отслеживаемых акций пока что пуст, но вы всегда можете добавить новые, нажав кнопку *'Добавить избр.'*").parseMode("Markdown");

    /**
     * Отправляем пользователю список его избранных акций
     *
     * @param chat_id идентификатор чата с пользователем
     */
    public static SendMessage sendSelectedStocks(String chat_id) {
        SendMessage.SendMessageBuilder smb = SendMessage.builder();

        String str_of_stocks;
        try {
            str_of_stocks = databaseHandler.getUserStocks(databaseHandler.user_table_name, chat_id);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            return smb
                    .text("У вас пока отсутствуют избранные акции.\n\nМожете добавить их, нажав на кнопку 'Добавить избр.'")
                    .chatId(chat_id)
                    .build();
        }

        if (str_of_stocks == null) {
            return listOfStocksEmpty.chatId(chat_id).build();
        }

        StringBuilder msg_text = new StringBuilder("Актуальный список отслеживаемых вами акций:\n\n");
        String[] stocks = str_of_stocks.split(";");
        Integer stock_num = 0;
        for (String stock : stocks) {
            stock_num++;
            Map<String, String> stock_data = apiHandler.getDataAboutStock(stock);

            String SHORTNAME = stock_data.get("SHORTNAME");
            String BID = stock_data.get("BID");
            float LASTTOPREVPRICE = Float.parseFloat(stock_data.get("LASTTOPREVPRICE"));

            String delta, price;
            if (!BID.equals("null")) {
                price = BID + " ₽";
                if (LASTTOPREVPRICE > 0)
                    delta = "дельта: " + new DecimalFormat("#0.00").format(LASTTOPREVPRICE) + "% ⬆️\n\n";
                else if (LASTTOPREVPRICE < 0) {
                    delta = "дельта: " + new DecimalFormat("#0.00").format(LASTTOPREVPRICE) + "% ⬇️\n\n";
                } else
                    delta = "дельта: 0%\n\n";
            } else {
                price = stock_data.get("PREVPRICE") + " ₽";
                delta = "биржа сейчас закрыта\n";
            }

            msg_text.append(String.format(
                    "[%d]:\tназвание: %s,\tтег: %s,\tстоимость: %s,\t%s",
                    stock_num, SHORTNAME, stock, price, delta
            ));
        }
        smb.text(msg_text.toString()).parseMode("Markdown").chatId(chat_id);

        return smb.build();
    }

    public static SendMessage sendHelp(Message msg) {

        return SendMessage.builder()
                .text("""
                        Это Бот для отслеживания выбранных вами акций, выставленных на торги Мосбиржей.

                        Здесь вы можете добавить любимые акции и отслеживать их цену и изменение цены последней сделки к последней цене предыдущего дня, выраженном в процентах.

                        Если биржа сейчас закрыта, то вы получите последнюю цену последней сделки нормального периода предыдущего торгового дня.

                        Ввести вы можете *до пяти* Избранных акций. Если хотите убрать лишние, можете нажать 'Изменить избр.', тогда появится клавиатура, где вы сможете выбрать акции для удаления.
                        """)
                .chatId(msg.getChatId().toString())
                .parseMode("Markdown")
                .build();
    }

    public static SendMessage changeSelectedStocks(String chat_id) {
        String user_selected_stocks;
        SendMessage.SendMessageBuilder smb = SendMessage.builder();
        try {
            user_selected_stocks = databaseHandler.getUserStocks(databaseHandler.user_table_name, chat_id);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            System.out.println(e);
            return smb
                    .text("У вас пока отсутствуют избранные акции.\n\nМожете добавить их, нажав на кнопку 'Добавить избр.'")
                    .chatId(chat_id)
                    .build();
        }

        if (user_selected_stocks == null) {
            return listOfStocksEmpty.chatId(chat_id).build();
        }

        String[] list_of_stocks = user_selected_stocks.split(";");

        return smb
                .text("Какую акцию хотели бы удалить из отслеживаемых?")
                .chatId(chat_id)
                .parseMode("MarkdownV2")
                .replyMarkup(createInlineKeyboardMarkup(list_of_stocks))
                .build();
    }

    public static SendMessage testSelectedStocks(Message msg) throws SQLException, ClassNotFoundException {
        String chat_id = String.valueOf(msg.getChatId());
        databaseHandler.setUserStock(databaseHandler.user_table_name, chat_id, "YNDX;SVCB;VTBR;AFKS");
        databaseHandler.printAllUsers(databaseHandler.user_table_name);
        return SendMessage.builder()
                .chatId(chat_id)
                .text("Успешно добавили тестовые значения – акции Яндекса, Совкомбанка, ВТБ банка и АФК Системы")
                .build();
    }

    public static SendMessage addSelectedStocks(Message msg) throws SQLException, ClassNotFoundException {
        String chat_id = String.valueOf(msg.getChatId());
        String[] text_split = msg.getText().split(":");

        if (text_split.length != 2) {
            return addSelectStocksHelper(chat_id);
        }

        String stock = text_split[1];

        if (!apiHandler.stockExist(stock)) {
            return stockNotExist(chat_id);
        }

        String user_selected_stocks = databaseHandler.getUserStocks(databaseHandler.user_table_name, chat_id);

        if (user_selected_stocks == null) {
            databaseHandler.setUserStock(databaseHandler.user_table_name, chat_id, stock);
        } else if (Arrays.asList(user_selected_stocks.split(";")).contains(stock)) {
            return SendMessage.builder()
                    .chatId(chat_id)
                    .text("Введённая вами акция уже отслеживается вами")
                    .build();
        } else if (user_selected_stocks.split(";").length <= 4) {
            databaseHandler.setUserStock(
                    databaseHandler.user_table_name, chat_id, String.join(";", user_selected_stocks + ";" + stock)
            );
        } else {
            return SendMessage.builder()
                    .chatId(chat_id)
                    .text("К сожалению, вы выбрали уже 5 из 5 возможных отслеживаний акций. При необходимости вы можете изменить список избранного, удалив некоторые акции из отслеживаемых")
                    .build();
        }

        return SendMessage.builder()
                .chatId(chat_id)
                .text(String.format("Успешно добавили акции %s в список отслеживаемых!", stock))
                .build();
    }

    public static SendMessage stockNotExist(String chat_id) {
        return SendMessage.builder()
                .text("К сожалению, не смогли найти акцию с таким тикером")
                .chatId(chat_id)
                .build();
    }

    public static SendMessage addSelectStocksHelper(String chat_id) {

        return SendMessage.builder()
                .text("Для того, чтобы добавить новую акцию, введите тикер актива в формате */add:{Тикер}*.\n\nНапример:\n/add:YNDX")
                .chatId(chat_id)
                .parseMode("Markdown")
                .build();
    }

    public static SendMessage sendMOEXLink(String chat_id) {
        List<InlineKeyboardButton> kbl = new ArrayList<>();
        kbl.add(InlineKeyboardButton.builder().text("Ход/Итоги торгов").url(
                "https://www.moex.com/ru/marketdata/#/mode=groups&group=4&collection=3&boardgroup=57&data_type=current&category=main").build());

        InlineKeyboardMarkup ikm = InlineKeyboardMarkup.builder().keyboardRow(kbl).build();

        return SendMessage.builder()
                .chatId(chat_id)
                .text("Для того, чтобы перейти на страницу Мосбиржи с актуальными торгуемыми акциями, вы можете перейти по следующей ссылке")
                .replyMarkup(ikm).build();
    }

    public static EditMessageReplyMarkup removeSelectedStock(String chat_id, String stock, Integer msg_id) {
        String[] str_of_list_of_stocks;
        try {
            str_of_list_of_stocks = databaseHandler.getUserStocks(databaseHandler.user_table_name, chat_id).split(";");
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }

        str_of_list_of_stocks = removeObject(str_of_list_of_stocks, stock);
        try {
            if (str_of_list_of_stocks.length > 0) {
                databaseHandler.setUserStock(databaseHandler.user_table_name, chat_id, String.join(";", str_of_list_of_stocks));
            } else {
                databaseHandler.setUserStock(databaseHandler.user_table_name, chat_id, null);
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return EditMessageReplyMarkup.builder()
                .chatId(chat_id)
                .messageId(msg_id)
                .replyMarkup(createInlineKeyboardMarkup(str_of_list_of_stocks))
                .build();
    }

    public static String[] removeObject(String[] arr, String strToRemove) {
        int i, j;
        for (i = 0, j = 0; j < arr.length; j++) {
            if (!arr[j].equals(strToRemove)) {
                arr[i++] = arr[j];
            }
        }
        ;
        return Arrays.copyOf(arr, i);
    }

    private static InlineKeyboardMarkup createInlineKeyboardMarkup(String[] list_of_stocks) {
        List<InlineKeyboardButton> kbl = new ArrayList<>();

        for (String stock : list_of_stocks) {
            kbl.add(InlineKeyboardButton.builder().text(stock).callbackData("/rm:" + stock).build());
        }

        kbl.add(InlineKeyboardButton.builder().text("Прекратить изменения").callbackData("/srm").build());

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder ikbmb = InlineKeyboardMarkup.builder();
        for (InlineKeyboardButton ikb : kbl) {
            ikbmb.keyboardRow(List.of(ikb));
        }

        return ikbmb.build();
    }
}
