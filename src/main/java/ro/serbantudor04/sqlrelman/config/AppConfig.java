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
            key = "release.dir",
            description = "The directory where new SQL release files are generated",
            defaultValue = "./releases"
    )
    public String releaseDirectory;

    @ConfigProperty(
            key = "db.driver",
            description = "JDBC Driver class (e.g., com.mysql.cj.jdbc.Driver, org.postgresql.Driver)",
            defaultValue = ""
    )
    public String dbDriver;

    @ConfigProperty(
            key = "db.url",
            description = "JDBC URL (e.g., jdbc:mysql://localhost:3306/mydb)",
            defaultValue = ""
    )
    public String dbUrl;

    @ConfigProperty(
            key = "db.user",
            description = "Database Username",
            defaultValue = ""
    )
    public String dbUser;

    @ConfigProperty(
            key = "db.password",
            description = "Database Password",
            defaultValue = ""
    )
    public String dbPassword;
}