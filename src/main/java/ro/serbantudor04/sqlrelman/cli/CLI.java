package ro.serbantudor04.sqlrelman.cli;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.cli.commands.BaseCommand;
import ro.serbantudor04.sqlrelman.cli.util.ClassScanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CLI {

    private final List<String> args;
    // Maps the command name (e.g., "help") to its instance
    private final Map<String, BaseCommand> commandRegistry;
    private static CLI instance;

    public static CLI getInstance() {
        if (instance == null) {
            instance = new CLI();
        }
        return instance;
    }

    private CLI() {
        this.args = new ArrayList<>();
        this.commandRegistry = new HashMap<>();
        loadCommandsDynamic();
    }

    public void parseArgs(String[] args) {
        this.args.addAll(List.of(args));
    }

    public void run() {
        if (args.isEmpty()) {
            System.out.println("No command provided.");
            printHelp();
            return;
        }

        // Extract the main command (e.g., "help", "connect")
        String commandName = args.get(0).toLowerCase();

        // Extract the rest of the arguments to pass to the command
        List<String> commandArgs = args.size() > 1 ? args.subList(1, args.size()) : new ArrayList<>();

        BaseCommand command = commandRegistry.get(commandName);

        if (command != null) {
            command.run(commandArgs);
        } else {
            System.out.println("Unknown command: " + commandName);
            printHelp();
        }
    }

    // Expose loaded commands so HelpCommand can read them
    public Collection<BaseCommand> getLoadedCommands() {
        return commandRegistry.values();
    }

    private void printHelp() {
        BaseCommand helpCmd = commandRegistry.get("help");
        if (helpCmd != null) {
            helpCmd.run(new ArrayList<>());
        } else {
            System.out.println("Help command not found. Available commands: " + commandRegistry.keySet());
        }
    }

    /**
     * Dynamically loads classes annotated with @Command using our custom ClassScanner.
     */
    private void loadCommandsDynamic() {
        String basePackage = "ro.serbantudor04.sqlrelman.cli.commands";
        List<Class<?>> commandClasses = ClassScanner.getClassesWithAnnotation(basePackage, Command.class);

        for (Class<?> clazz : commandClasses) {
            try {
                // Ensure it extends BaseCommand before casting
                if (BaseCommand.class.isAssignableFrom(clazz)) {
                    BaseCommand cmd = (BaseCommand) clazz.getDeclaredConstructor().newInstance();
                    cmd.setCli(this); // Inject the CLI context
                    commandRegistry.put(cmd.getName().toLowerCase(), cmd);
                }
            } catch (Exception e) {
                System.err.println("Failed to instantiate command class: " + clazz.getName());
                e.printStackTrace();
            }
        }
    }
}