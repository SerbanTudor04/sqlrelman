package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.cli.annotations.ConfigProperty;
import ro.serbantudor04.sqlrelman.config.AppConfig;
import ro.serbantudor04.sqlrelman.config.ConfigManager;
import ro.serbantudor04.sqlrelman.engine.DatabaseManager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import java.util.Scanner;

@Command
public class SetupCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        Scanner scanner = new Scanner(System.in);
        AppConfig config = AppConfig.getInstance();
        ConfigManager manager = ConfigManager.getInstance();

        manager.bind(config);

        System.out.println("=========================================");
        System.out.println("          SQLRelMan Setup Wizard         ");
        System.out.println("=========================================");
        System.out.println("Press [Enter] to keep the current value.");

        Field[] fields = config.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(ConfigProperty.class)) continue;

            ConfigProperty annotation = field.getAnnotation(ConfigProperty.class);
            field.setAccessible(true);

            try {
                String currentValue = (String) field.get(config);
                if (currentValue == null || currentValue.isEmpty()) {
                    currentValue = annotation.defaultValue();
                }

                System.out.println("\n--- " + annotation.description() + " ---");
                System.out.println("Current: " + currentValue);
                System.out.print("New value: ");

                String input = scanner.nextLine().trim();

                if (!input.isEmpty()) {
                    field.set(config, input);
                }

            } catch (IllegalAccessException e) {
                System.err.println("Error accessing configuration field: " + field.getName());
            }
        }

        manager.save(config);
        System.out.println("\nConfiguration saved.");

        System.out.println("Testing database connection...");
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("SUCCESS: Connected to the database!");
        } catch (Exception e) {
            System.err.println("WARNING: Database connection failed. Please check your settings.");
            System.err.println("Reason: " + e.getMessage());
        }

        System.out.println("=========================================");
    }

    @Override
    public String getHelp() { return "Interactive setup for database and release properties."; }
    @Override
    public String getName() { return "setup"; }
    @Override
    public String getUsage() { return "setup"; }
    @Override
    public String getDescription() { return "Configure the application."; }
    @Override
    public String getVersion() { return "1.0"; }
    @Override
    public String getAuthor() { return "Serban Tudor"; }
    @Override
    public String getDate() { return "2026-03-31"; }
    @Override
    public String getLicense() { return "MIT"; }
}