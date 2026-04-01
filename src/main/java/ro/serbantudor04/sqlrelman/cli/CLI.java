package ro.serbantudor04.sqlrelman.cli;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.cli.commands.BaseCommand;
import ro.serbantudor04.sqlrelman.cli.util.ClassScanner;
import ro.serbantudor04.sqlrelman.config.AppConfig;
import ro.serbantudor04.sqlrelman.config.ConfigManager;

import java.util.*;

public class CLI {

    private final List<String> args;
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
        initConfig();
        loadCommandsDynamic();
    }

    private void initConfig() {
        ConfigManager.getInstance().bind(AppConfig.getInstance());
    }

    public void parseArgs(String[] args) {
        this.args.addAll(List.of(args));
    }

    public void run() {
        // No args or explicit interactive flag → drop into the shell
        if (args.isEmpty()
                || args.contains("--interactive")
                || args.contains("-i")) {
            new InteractiveShell(this).start();
            return;
        }

        String commandName = args.get(0).toLowerCase();
        List<String> commandArgs = args.size() > 1
                ? args.subList(1, args.size())
                : new ArrayList<>();

        BaseCommand command = commandRegistry.get(commandName);
        if (command != null) {
            command.run(commandArgs);
        } else {
            System.out.println("Unknown command: " + commandName);
            printHelp();
        }
    }

    /** Used by InteractiveShell to dispatch commands by name. */
    public BaseCommand findCommand(String name) {
        return commandRegistry.get(name.toLowerCase());
    }

    public Collection<BaseCommand> getLoadedCommands() {
        return commandRegistry.values();
    }

    private void printHelp() {
        BaseCommand helpCmd = commandRegistry.get("help");
        if (helpCmd != null) {
            helpCmd.run(new ArrayList<>());
        } else {
            System.out.println("Available commands: " + commandRegistry.keySet());
        }
    }

    private void loadCommandsDynamic() {
        String basePackage = "ro.serbantudor04.sqlrelman.cli.commands";
        List<Class<?>> commandClasses = ClassScanner.getClassesWithAnnotation(basePackage, Command.class);

        for (Class<?> clazz : commandClasses) {
            try {
                if (BaseCommand.class.isAssignableFrom(clazz)) {
                    BaseCommand cmd = (BaseCommand) clazz.getDeclaredConstructor().newInstance();
                    cmd.setCli(this);
                    commandRegistry.put(cmd.getName().toLowerCase(), cmd);
                }
            } catch (Exception e) {
                System.err.println("Failed to instantiate command: " + clazz.getName());
                e.printStackTrace();
            }
        }
    }
}