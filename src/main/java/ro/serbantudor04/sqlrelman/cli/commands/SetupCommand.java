package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.cli.annotations.ConfigProperty;
import ro.serbantudor04.sqlrelman.config.AppConfig;
import ro.serbantudor04.sqlrelman.config.ConfigManager;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Scanner;

@Command
public class SetupCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        Scanner scanner = new Scanner(System.in);
        AppConfig config = AppConfig.getInstance();
        ConfigManager manager = ConfigManager.getInstance();

        // Ensure config has the latest file values before prompting
        manager.bind(config);

        System.out.println("=========================================");
        System.out.println("          Dynamic Interactive Setup      ");
        System.out.println("=========================================");
        System.out.println("Press [Enter] to keep the current value.");
        System.out.println();

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

                System.out.println("--- " + annotation.description() + " ---");
                System.out.println("Current: " + currentValue);
                System.out.print("New value: ");

                String input = scanner.nextLine().trim();

                if (!input.isEmpty()) {
                    field.set(config, input);
                }
                System.out.println();

            } catch (IllegalAccessException e) {
                System.err.println("Error accessing configuration field: " + field.getName());
            }
        }

        // Save the dynamically updated object back to the properties file
        manager.save(config);

        System.out.println("=========================================");
        System.out.println("Setup complete! All configurations saved.");
    }

    @Override
    public String getHelp() {
        return "Runs a dynamically generated interactive setup wizard for app properties.";
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