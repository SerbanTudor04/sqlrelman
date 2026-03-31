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
            key = "db.url",
            description = "The database connection URL",
            defaultValue = "jdbc:mysql://localhost:3306/relman"
    )
    public String databaseUrl;

    @ConfigProperty(
            key = "author.name",
            description = "The default author name to embed in generated SQL scripts",
            defaultValue = "Unknown"
    )
    public String authorName;
}