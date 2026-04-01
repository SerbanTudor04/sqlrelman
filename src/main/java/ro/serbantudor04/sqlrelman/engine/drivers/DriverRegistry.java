package ro.serbantudor04.sqlrelman.engine.drivers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of supported database types and their JDBC driver metadata.
 * Drivers are downloaded from Maven Central on demand.
 */
public class DriverRegistry {

    public record DriverInfo(
            String displayName,
            String driverClass,
            String groupId,
            String artifactId,
            String version,
            String urlTemplate   // example URL shown to the user during setup
    ) {
        /** Returns the Maven Central download URL for this driver JAR. */
        public String mavenJarUrl() {
            String groupPath = groupId.replace('.', '/');
            return String.format(
                    "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
                    groupPath, artifactId, version, artifactId, version
            );
        }

        /** Returns the local filename for the cached JAR. */
        public String jarFileName() {
            return artifactId + "-" + version + ".jar";
        }
    }

    // Key = short type identifier used in CLI and config
    private static final Map<String, DriverInfo> REGISTRY = new LinkedHashMap<>();

    static {
        REGISTRY.put("mysql", new DriverInfo(
                "MySQL",
                "com.mysql.cj.jdbc.Driver",
                "com.mysql.cj",
                "mysql-connector-j",
                "9.1.0",
                "jdbc:mysql://localhost:3306/{database}"
        ));

        REGISTRY.put("postgresql", new DriverInfo(
                "PostgreSQL",
                "org.postgresql.Driver",
                "org.postgresql",
                "postgresql",
                "42.7.4",
                "jdbc:postgresql://localhost:5432/{database}"
        ));

        REGISTRY.put("mariadb", new DriverInfo(
                "MariaDB",
                "org.mariadb.jdbc.Driver",
                "org.mariadb.jdbc",
                "mariadb-java-client",
                "3.4.1",
                "jdbc:mariadb://localhost:3306/{database}"
        ));

        REGISTRY.put("sqlserver", new DriverInfo(
                "Microsoft SQL Server",
                "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                "com.microsoft.sqlserver",
                "mssql-jdbc",
                "12.8.1.jre11",
                "jdbc:sqlserver://localhost:1433;databaseName={database}"
        ));

        REGISTRY.put("oracle", new DriverInfo(
                "Oracle Database",
                "oracle.jdbc.OracleDriver",
                "com.oracle.database.jdbc",
                "ojdbc11",
                "23.6.0.24.10",
                "jdbc:oracle:thin:@localhost:1521:{database}"
        ));

        REGISTRY.put("sqlite", new DriverInfo(
                "SQLite",
                "org.sqlite.JDBC",
                "org.xerial",
                "sqlite-jdbc",
                "3.47.1.0",
                "jdbc:sqlite:/path/to/{database}.db"
        ));

        REGISTRY.put("h2", new DriverInfo(
                "H2 (embedded)",
                "org.h2.Driver",
                "com.h2database",
                "h2",
                "2.3.232",
                "jdbc:h2:~/test"
        ));
    }

    public static Set<String> supportedTypes() {
        return REGISTRY.keySet();
    }

    public static DriverInfo get(String type) {
        return REGISTRY.get(type.toLowerCase());
    }

    public static boolean isSupported(String type) {
        return REGISTRY.containsKey(type.toLowerCase());
    }

    /** Returns all entries for display purposes. */
    public static Map<String, DriverInfo> all() {
        return REGISTRY;
    }
}