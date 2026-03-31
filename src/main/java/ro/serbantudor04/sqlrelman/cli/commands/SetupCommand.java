package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.config.ConfigManager;

import java.io.File;
import java.util.List;
import java.util.Scanner;

@Command
public class SetupCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        Scanner scanner = new Scanner(System.in);
        ConfigManager config = ConfigManager.getInstance();

        System.out.println("=========================================");
        System.out.println("          SQLRelMan Interactive Setup    ");
        System.out.println("=========================================");

        // --- Setting: Release Directory ---
        String currentReleaseDir = config.getProperty("release.dir", "Not set");
        System.out.println("Current release directory: " + currentReleaseDir);
        System.out.print("Enter new release directory (or press Enter to keep current): ");

        String newReleaseDir = scanner.nextLine().trim();

        if (!newReleaseDir.isEmpty()) {
            File dir = new File(newReleaseDir);

            if (!dir.exists()) {
                System.out.print("Directory does not exist. Create it now? (y/n): ");
                String createDir = scanner.nextLine().trim().toLowerCase();

                if (createDir.equals("y") || createDir.equals("yes")) {
                    if (dir.mkdirs()) {
                        System.out.println("Directory created.");
                        config.setProperty("release.dir", dir.getAbsolutePath());
                    } else {
                        System.out.println("Failed to create directory. Configuration not saved.");
                    }
                }
            } else if (!dir.isDirectory()) {
                System.out.println("Path exists but is not a directory. Configuration not saved.");
            } else {
                config.setProperty("release.dir", dir.getAbsolutePath());
            }
        }

        // --- Setting: Example for Database URL (You can uncomment or modify later) ---
        /*
        String currentDbUrl = config.getProperty("db.url", "Not set");
        System.out.println("\nCurrent database URL: " + currentDbUrl);
        System.out.print("Enter new database URL (or press Enter to keep current): ");
        String newDbUrl = scanner.nextLine().trim();
        if (!newDbUrl.isEmpty()) {
            config.setProperty("db.url", newDbUrl);
        }
        */

        // Save all the updated properties to the file
        config.saveConfig();

        System.out.println("=========================================");
        System.out.println("Setup complete! Configuration saved.");
    }

    @Override
    public String getHelp() {
        return "Runs an interactive setup wizard to configure the application's properties (e.g., release directory).";
    }

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getUsage() {
        return "setup";
    }

    @Override
    public String getDescription() {
        return "Interactive configuration setup.";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getAuthor() {
        return "Serban Tudor";
    }

    @Override
    public String getDate() {
        return "2026-03-31";
    }

    @Override
    public String getLicense() {
        return "MIT";
    }
}