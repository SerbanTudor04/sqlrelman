package ro.serbantudor04.sqlrelman;

import ro.serbantudor04.sqlrelman.cli.CLI;

public class SqlRelMan {


    public static void main(String[] args) {
        CLI cli = CLI.getInstance();
        cli.parseArgs(args);
        cli.run();
    }
}
