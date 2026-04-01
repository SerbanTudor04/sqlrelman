package ro.serbantudor04.sqlrelman.engine;

import ro.serbantudor04.sqlrelman.config.AppConfig;
import ro.serbantudor04.sqlrelman.engine.drivers.DriverDownloader;
import ro.serbantudor04.sqlrelman.engine.drivers.DriverRegistry;
import ro.serbantudor04.sqlrelman.engine.drivers.DynamicDriverLoader;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    /**
     * Opens a JDBC connection using the configured database type.
     *
     * If a driver JAR for the configured db.type is cached locally it is used directly.
     * If not, an informative error is thrown asking the user to run 'setup' first.
     *
     * The driver class can be overridden via db.driver in the config.
     */
    public static Connection getConnection() throws Exception {
        AppConfig config = AppConfig.getInstance();

        if (config.dbUrl == null || config.dbUrl.isEmpty()) {
            throw new SQLException("Database URL is not configured. Run 'setup' first.");
        }

        // Resolve driver info from registry unless the user set a manual driver class
        String driverClass = config.dbDriver;
        File driverJar = null;

        if (config.dbType != null && !config.dbType.isEmpty()) {
            DriverRegistry.DriverInfo info = DriverRegistry.get(config.dbType);

            if (info == null) {
                throw new SQLException("Unknown database type: '" + config.dbType
                        + "'. Supported types: " + DriverRegistry.supportedTypes());
            }

            // Use the registry driver class unless manually overridden
            if (driverClass == null || driverClass.isEmpty()) {
                driverClass = info.driverClass();
            }

            driverJar = DriverDownloader.getCachedDriver(info);

            if (driverJar == null) {
                throw new SQLException(
                        "Driver JAR for '" + info.displayName() + "' is not downloaded yet.\n" +
                                "Run 'setup' to download it, or run 'driver download " + config.dbType + "' manually.");
            }
        }

        if (driverClass == null || driverClass.isEmpty()) {
            throw new SQLException(
                    "No driver class found. Set db.type or db.driver in your configuration (run 'setup').");
        }

        if (driverJar != null) {
            // Use dynamic loader for downloaded drivers
            return DynamicDriverLoader.connect(driverJar, driverClass,
                    config.dbUrl, config.dbUser, config.dbPassword);
        } else {
            // Fall back to standard Class.forName for drivers on the classpath
            Class.forName(driverClass);
            return java.sql.DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword);
        }
    }
}