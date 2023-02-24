package searchengine.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionService {
    private final static String dbName = "search_engine";
    private final static String dbUser = "root";
    private final static String dbPass = "testtest";

    private static Connection connection;
    public static Connection connect() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/" + dbName +
                                "?user=" + dbUser + "&password=" + dbPass);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}
