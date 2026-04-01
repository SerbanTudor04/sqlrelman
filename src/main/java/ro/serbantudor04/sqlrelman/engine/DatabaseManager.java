package ro.serbantudor04.sqlrelman.engine;

import ro.serbantudor04.sqlrelman.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        AppConfig config = AppConfig.getInstance();

        if (config.dbUrl == null || config.dbUrl.isEmpty()) {
            throw new SQLException("Database URL is not configured. Run 'setup' command.");
        }

        if (config.dbDriver != null && !config.dbDriver.isEmpty()) {
            Class.forName(config.dbDriver);
        }

        return DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword);
    }
}