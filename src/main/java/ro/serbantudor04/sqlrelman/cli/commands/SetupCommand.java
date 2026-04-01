package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.config.AppConfig;
import ro.serbantudor04.sqlrelman.config.ConfigManager;
import ro.serbantudor04.sqlrelman.engine.DatabaseManager;
import ro.serbantudor04.sqlrelman.engine.drivers.DriverDownloader;
import ro.serbantudor04.sqlrelman.engine.drivers.DriverRegistry;
import ro.serbantudor04.sqlrelman.engine.drivers.DriverRegistry.DriverInfo;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive setup wizard.
 *
 * Flow:
 *  1. Releases directory
 *  2. Choose database type from a numbered menu
 *  3. Show the URL template for that type; let user fill in the real URL
 *  4. Username / password
 *  5. Download the driver JAR if not already cached
 *  6. Test the connection
 */
@Command
public class SetupCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        Scanner scanner = new Scanner(System.in);
        AppConfig config = AppConfig.getInstance();
        ConfigManager manager = ConfigManager.getInstance();
        manager.bind(config);

        printBanner("SQLRelMan Setup Wizard");
        System.out.println("Press [Enter] to keep the current value.\n");

        // --- 1. Releases directory ---
        config.releasesDirectory = prompt(scanner,
                "Releases directory",
                config.releasesDirectory,
                "./releases");

        // --- 2. Database type ---
        String chosenType = promptDbType(scanner, config.dbType);
        config.dbType = chosenType;

        DriverInfo driverInfo = DriverRegistry.get(chosenType);

        // --- 3. JDBC URL ---
        String urlHint = driverInfo.urlTemplate();
        System.out.println("\n  URL template : " + urlHint);
        config.dbUrl = prompt(scanner, "JDBC URL", config.dbUrl, urlHint);

        // --- 4. Credentials ---
        if (requiresCredentials(chosenType)) {
            config.dbUser     = prompt(scanner, "Username", config.dbUser, "");
            config.dbPassword = promptPassword(scanner, "Password", config.dbPassword);
        } else {
            System.out.println("  (SQLite and H2 file-mode do not require credentials — skipping)");
        }

        // --- 5. Driver class override (advanced, optional) ---
        System.out.println("\n  Default driver class : " + driverInfo.driverClass());
        String driverOverride = prompt(scanner,
                "Driver class override [leave blank to use default]",
                config.dbDriver,
                "");
        config.dbDriver = driverOverride.equals(driverInfo.driverClass()) ? "" : driverOverride;

        // --- Save config ---
        manager.save(config);
        System.out.println("\nConfiguration saved.");

        // --- 6. Download driver ---
        System.out.println();
        printBanner("Driver Download");
        System.out.printf("  Database  : %s%n", driverInfo.displayName());
        System.out.printf("  Artifact  : %s:%s:%s%n",
                driverInfo.groupId(), driverInfo.artifactId(), driverInfo.version());

        boolean alreadyCached = DriverDownloader.getCachedDriver(driverInfo) != null;
        boolean shouldDownload = true;

        if (alreadyCached) {
            String answer = prompt(scanner,
                    "Driver already cached. Re-download? [y/N]", "", "N");
            shouldDownload = answer.equalsIgnoreCase("y");
        }

        if (shouldDownload) {
            var jar = DriverDownloader.ensureDriver(driverInfo, alreadyCached);
            if (jar == null) {
                System.err.println("Driver download failed. Check your internet connection and try again.");
                System.err.println("You can retry later with: driver download " + chosenType);
                return;
            }
        }

        // --- 7. Test connection ---
        System.out.println();
        printBanner("Connection Test");
        System.out.print("  Testing connection ... ");
        try (Connection conn = DatabaseManager.getConnection()) {
            String dbProductName = conn.getMetaData().getDatabaseProductName();
            String dbVersion     = conn.getMetaData().getDatabaseProductVersion();
            System.out.println("SUCCESS");
            System.out.println("  Connected to : " + dbProductName + " " + dbVersion);
        } catch (Exception e) {
            System.out.println("FAILED");
            System.err.println("  Reason: " + e.getMessage());
            System.err.println("  Check your URL, credentials, and that the database server is running.");
        }

        printBanner("Setup Complete");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Displays a numbered menu of supported database types and returns the chosen key.
     */
    private String promptDbType(Scanner scanner, String currentType) {
        Map<String, DriverInfo> all = DriverRegistry.all();
        List<String> keys = new ArrayList<>(all.keySet());

        System.out.println("\n--- Database Type ---");
        System.out.println("  Supported databases:");
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            DriverInfo info = all.get(key);
            String marker = key.equals(currentType) ? " [current]" : "";
            System.out.printf("    %2d. %-12s %s%s%n", i + 1, key, info.displayName(), marker);
        }

        while (true) {
            String currentLabel = currentType != null && !currentType.isEmpty()
                    ? currentType : "(none)";
            System.out.print("  Choose [1-" + keys.size() + "] (current: " + currentLabel + "): ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty() && currentType != null && !currentType.isEmpty()) {
                return currentType; // keep existing
            }

            // Accept number or direct type name
            try {
                int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < keys.size()) {
                    String chosen = keys.get(idx);
                    System.out.println("  Selected: " + DriverRegistry.get(chosen).displayName());
                    return chosen;
                }
            } catch (NumberFormatException ignored) {
                if (DriverRegistry.isSupported(input)) {
                    return input.toLowerCase();
                }
            }

            System.err.println("  Invalid choice. Enter a number between 1 and " + keys.size() + ".");
        }
    }

    private String prompt(Scanner scanner, String label, String current, String defaultValue) {
        String display = (current != null && !current.isEmpty()) ? current : defaultValue;
        System.out.printf("  %-40s [%s]: ", label, display);
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) return (current != null && !current.isEmpty()) ? current : defaultValue;
        return input;
    }

    private String promptPassword(Scanner scanner, String label, String current) {
        // Mask password if running in a real terminal (Console); fall back to Scanner
        java.io.Console console = System.console();
        if (console != null) {
            System.out.printf("  %-40s [****]: ", label);
            char[] pwd = console.readPassword();
            if (pwd == null || pwd.length == 0) return current != null ? current : "";
            return new String(pwd);
        }
        return prompt(scanner, label + " (input visible)", current, "");
    }

    private boolean requiresCredentials(String dbType) {
        return !dbType.equals("sqlite") && !dbType.equals("h2");
    }

    private void printBanner(String title) {
        String line = "=".repeat(50);
        System.out.println(line);
        System.out.printf("  %s%n", title);
        System.out.println(line);
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Override public String getName()        { return "setup"; }
    @Override public String getDescription() { return "Interactive wizard to configure database and download its JDBC driver."; }
    @Override public String getUsage()       { return "setup"; }
    @Override public String getHelp()        { return "Guides you through configuring the database type, URL, credentials, and automatically downloads the correct JDBC driver."; }
    @Override public String getVersion()     { return "2.0"; }
    @Override public String getAuthor()      { return "Serban Tudor"; }
    @Override public String getDate()        { return "2026-04-01"; }
    @Override public String getLicense()     { return "MIT"; }
}