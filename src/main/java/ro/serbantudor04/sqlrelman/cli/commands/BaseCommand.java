package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.CLI;
import java.util.List;

public abstract class BaseCommand {
    protected CLI cli; // Gives commands access to the main CLI instance

    public void setCli(CLI cli) {
        this.cli = cli;
    }

    // Pass the parsed arguments to the command execution
    public abstract void run(List<String> args);

    public abstract String getHelp();
    public abstract String getName();
    public abstract String getUsage();
    public abstract String getDescription();
    public abstract String getVersion();
    public abstract String getAuthor();
    public abstract String getDate();
    public abstract String getLicense();
}