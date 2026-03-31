package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import java.util.List;

@Command
public class NewReleaseCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        System.out.println("Running new_release command...");
    }

    @Override
    public String getHelp() {
        return "Makes a new release version.";
    }

    @Override
    public String getName() {
        return "new_release";
    }

    @Override
    public String getUsage() {
        return "new_release <major> <minor> <patch>";
    }

    @Override
    public String getDescription() {
        return "Makes a new release version.";
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