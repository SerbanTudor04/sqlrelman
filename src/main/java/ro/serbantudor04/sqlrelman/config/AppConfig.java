package ro.serbantudor04.sqlrelman.config;

import ro.serbantudor04.sqlrelman.cli.annotations.ConfigProperty;

public class AppConfig {

    private static AppConfig instance;

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private AppConfig() {}

    @ConfigProperty(
            key = "releases.dir",
            description = "The directory where SQL release directories are stored",
            defaultValue = "./releases"
    )
    public String releasesDirectory;

    @ConfigProperty(
            key = "db.type",
            description = "Database type (mysql, postgresql, mariadb, sqlserver, oracle, sqlite, h2)",
            defaultValue = ""
    )
    public String dbType;

    // dbDriver is now derived from dbType at runtime; kept for manual overrides.
    @ConfigProperty(
            key = "db.driver",
            description = "JDBC Driver class override (leave blank to use the default for your db.type)",
            defaultValue = ""
    )
    public String dbDriver;

    @ConfigProperty(
            key = "db.url",
            description = "JDBC URL (e.g., jdbc:postgresql://localhost:5432/mydb)",
            defaultValue = ""
    )
    public String dbUrl;

    @ConfigProperty(
            key = "db.user",
            description = "Database username",
            defaultValue = ""
    )
    public String dbUser;

    @ConfigProperty(
            key = "db.password",
            description = "Database password",
            defaultValue = ""
    )
    public String dbPassword;
}