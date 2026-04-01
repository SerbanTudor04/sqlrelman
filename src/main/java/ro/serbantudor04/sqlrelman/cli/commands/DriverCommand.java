package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.engine.drivers.DriverDownloader;
import ro.serbantudor04.sqlrelman.engine.drivers.DriverRegistry;
import ro.serbantudor04.sqlrelman.engine.drivers.DriverRegistry.DriverInfo;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Handles: driver list
 *          driver download <type> [--force]
 *          driver info <type>
 */
@Command
public class DriverCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        if (args.isEmpty()) {
            printUsageHint();
            return;
        }

        String subcommand = args.get(0).toLowerCase();
        List<String> rest = args.size() > 1 ? args.subList(1, args.size()) : List.of();

        switch (subcommand) {
            case "list"     -> handleList();
            case "download" -> handleDownload(rest);
            case "info"     -> handleInfo(rest);
            default -> {
                System.err.println("Unknown driver subcommand: " + subcommand);
                printUsageHint();
            }
        }
    }

    // -------------------------------------------------------------------------

    private void handleList() {
        Map<String, DriverInfo> all = DriverRegistry.all();

        System.out.println("=== Supported Database Drivers ===");
        System.out.printf("  %-14s %-22s %-10s %s%n", "TYPE", "DISPLAY NAME", "VERSION", "CACHED");
        System.out.println("  " + "-".repeat(66));

        for (Map.Entry<String, DriverInfo> entry : all.entrySet()) {
            DriverInfo info = entry.getValue();
            File cached = DriverDownloader.getCachedDriver(info);
            String status = cached != null ? "YES  (" + cached.getName() + ")" : "no";
            System.out.printf("  %-14s %-22s %-10s %s%n",
                    entry.getKey(), info.displayName(), info.version(), status);
        }
    }

    private void handleDownload(List<String> args) {
        if (args.isEmpty()) {
            System.err.println("Usage: driver download <type> [--force]");
            System.err.println("  Supported types: " + DriverRegistry.supportedTypes());
            return;
        }

        String type = args.get(0).toLowerCase();
        boolean force = args.contains("--force");

        DriverInfo info = DriverRegistry.get(type);
        if (info == null) {
            System.err.println("Unknown database type: '" + type + "'");
            System.err.println("Supported types: " + DriverRegistry.supportedTypes());
            return;
        }

        System.out.println("Driver : " + info.displayName());
        System.out.println("JAR    : " + info.artifactId() + "-" + info.version() + ".jar");

        File jar = DriverDownloader.ensureDriver(info, force);
        if (jar != null) {
            System.out.println("Ready  : " + jar.getAbsolutePath());
        } else {
            System.err.println("Download failed. Check your internet connection.");
        }
    }

    private void handleInfo(List<String> args) {
        if (args.isEmpty()) {
            System.err.println("Usage: driver info <type>");
            return;
        }

        String type = args.get(0).toLowerCase();
        DriverInfo info = DriverRegistry.get(type);
        if (info == null) {
            System.err.println("Unknown database type: '" + type + "'");
            return;
        }

        File cached = DriverDownloader.getCachedDriver(info);

        System.out.println("=== Driver Info: " + type + " ===");
        System.out.println("  Display name  : " + info.displayName());
        System.out.println("  Driver class  : " + info.driverClass());
        System.out.println("  Maven coords  : " + info.groupId() + ":" + info.artifactId() + ":" + info.version());
        System.out.println("  Download URL  : " + info.mavenJarUrl());
        System.out.println("  URL template  : " + info.urlTemplate());
        System.out.println("  Cached locally: " + (cached != null ? cached.getAbsolutePath() : "NO"));
    }

    private void printUsageHint() {
        System.out.println("Usage:");
        System.out.println("  driver list");
        System.out.println("  driver download <type> [--force]");
        System.out.println("  driver info <type>");
        System.out.println();
        System.out.println("  Supported types: " + DriverRegistry.supportedTypes());
    }

    // -------------------------------------------------------------------------

    @Override public String getName()        { return "driver"; }
    @Override public String getDescription() { return "Manage JDBC drivers (list, download, info)."; }
    @Override public String getUsage()       { return "driver <list|download|info> [args...]"; }
    @Override public String getHelp()        { return "Download and manage JDBC driver JARs from Maven Central."; }
    @Override public String getVersion()     { return "1.0"; }
    @Override public String getAuthor()      { return "Serban Tudor"; }
    @Override public String getDate()        { return "2026-04-01"; }
    @Override public String getLicense()     { return "MIT"; }
}