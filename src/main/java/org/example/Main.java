package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.sql.*;

public class Main {
    public static void main(String[] args) throws TelegramApiException, SQLException, ClassNotFoundException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();
        botsApi.registerBot(bot);
        var msg_text = "We are started!";
        bot.sendText(Bot.Vlad_id, msg_text);

        // Инициализируем Таблицы, если она отсутствовала ранее
        databaseHandler.createTable(databaseHandler.user_table_name);
        System.out.println(msg_text);

        databaseHandler.printAllUsers(databaseHandler.user_table_name);

    }
}