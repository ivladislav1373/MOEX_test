package org.example;

import org.h2.jdbc.JdbcSQLTransientException;

import java.sql.*;
//Тест
// Привет, Тимур!
public class databaseHandler {
//    public static final String DB_URL = "jdbc:h2:mem:db";
    public static final String DB_URL = "jdbc:h2:file:C:\\teest\\db_4_moex_checker";
    public static final String DB_DRIVER = "org.h2.Driver";
    public static final String user_table_name = "Users";
    public static final String Vlad_id = String.valueOf(Bot.Vlad_id);
    public static final String Timur_id = String.valueOf(Bot.Vlad_id);


    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        createTable(user_table_name);
//        printAllUsers(user_table_name);
//        System.out.println(getUserStocks(user_table_name, Vlad_id));
    }

    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName(DB_DRIVER);
        return DriverManager.getConnection(DB_URL);
    }

    public static void createTable(String table_name) throws ClassNotFoundException, SQLException {
        Connection con = getConnection();
        Statement st = con.createStatement();
        String sql = String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    chat_id VARCHAR(17) PRIMARY KEY,
                    list_of_stocks VARCHAR(255)
                );
                """, table_name);
//        st.executeUpdate(sql);
        st.execute(sql);
        st.close();
        con.close();
    }

    public static void addNewUser(String table_name, String chat_id) throws ClassNotFoundException, SQLException {
        final String sql_count = String.format("""
                SELECT COUNT(*)
                FROM %s
                WHERE chat_id = '%s'
                """, table_name, chat_id);

        Connection con = getConnection();

        Statement st = con.createStatement();
        ResultSet res = st.executeQuery(sql_count);
        res.next();
        if (res.getString(1).equals("0")) {
            final String sql = String.format("""
                    INSERT INTO %s
                        (chat_id, list_of_stocks)
                    VALUES
                        (%s, NULL)
                    """, table_name, chat_id);
            st.executeUpdate(sql);
        }
        res.close();
        st.close();
        con.close();
    }

    public static String getUserStocks(String table_name, String chat_id) throws ClassNotFoundException, SQLException, JdbcSQLTransientException {
        final String sql = String.format("""
                SELECT list_of_stocks
                FROM %s
                WHERE chat_id = %s
                """, table_name, chat_id);

        Connection con = getConnection();

        Statement st = con.createStatement();
        ResultSet res = st.executeQuery(sql);
        res.next();
        String out;
        try {
            out = res.getString(1);
        } catch (JdbcSQLTransientException e) {
            throw e;
        } finally {
            res.close();
            st.close();
            con.close();
        }
        return out;
    }

    public static void setUserStock(String table_name, String chat_id, String stocks) throws SQLException, ClassNotFoundException {
        if (stocks != null) {
            stocks = "'" + stocks + "'";
        } else {
            stocks = "NULL";
        }
        final String sql = String.format("""
                UPDATE %s
                    SET list_of_stocks = %s
                    WHERE chat_id = %s;
                """, table_name, stocks, chat_id
        );

        System.out.println(sql);
        Connection con = getConnection();

        Statement st = con.createStatement();
        st.executeUpdate(sql);
        st.close();
        con.close();
    }

    public static void printAllUsers(String table_name) throws ClassNotFoundException, SQLException {
        final String sql = String.format("""
                SELECT * FROM %s
                """, table_name);

        Connection con = getConnection();

        Statement st = con.createStatement();
        ResultSet res = st.executeQuery(sql);

        while (res.next()) {
            System.out.printf(
                    "Chat id is\t%s, list of stacks is\t%s%n",
                    res.getString(1), res.getString(2)
            );
        }
        res.close();
        st.close();
        con.close();
    }

}
