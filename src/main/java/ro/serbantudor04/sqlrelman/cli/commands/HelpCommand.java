package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import java.util.List;

@Command
public class HelpCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        System.out.println("=========================================");
        System.out.println("          SQLRelMan Command Help         ");
        System.out.println("=========================================");
        System.out.println("Available commands:");

        // Dynamically print all loaded commands
        for (BaseCommand cmd : cli.getLoadedCommands()) {
            System.out.printf("  %-15s : %s%n", cmd.getName(), cmd.getDescription());
        }
        System.out.println("=========================================");
        System.out.println("Type '<command> --help' for more details.");
    }

    @Override
    public String getHelp() {
        return "Displays a list of all available commands and their descriptions.";
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getUsage() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Shows the help menu.";
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