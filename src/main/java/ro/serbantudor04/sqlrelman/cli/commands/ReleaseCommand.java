package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.engine.models.Patch;
import ro.serbantudor04.sqlrelman.engine.models.Release;

import java.util.List;

/**
 * Handles: release create <version> <description>
 *          release list
 *          release delete <version>
 *          release info <version>
 */
@Command
public class ReleaseCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        if (args.isEmpty()) {
            printUsageHint();
            return;
        }

        String subcommand = args.get(0).toLowerCase();
        List<String> rest = args.size() > 1 ? args.subList(1, args.size()) : List.of();

        switch (subcommand) {
            case "create" -> handleCreate(rest);
            case "list"   -> handleList();
            case "delete" -> handleDelete(rest);
            case "info"   -> handleInfo(rest);
            default -> {
                System.err.println("Unknown release subcommand: " + subcommand);
                printUsageHint();
            }
        }
    }

    // -------------------------------------------------------------------------

    private void handleCreate(List<String> args) {
        if (args.isEmpty()) {
            System.err.println("Usage: release create <version> [description]");
            return;
        }
        String version = args.get(0);
        String description = args.size() > 1
                ? String.join(" ", args.subList(1, args.size()))
                : "Release " + version;

        Release release = new Release(version, description);
        if (release.create()) {
            System.out.println("Release '" + version + "' created at: " + release.getReleaseDir().getAbsolutePath());
        }
    }

    private void handleList() {
        List<Release> releases = Release.listAll();
        if (releases.isEmpty()) {
            System.out.println("No releases found.");
            return;
        }

        System.out.println("=== Releases ===");
        for (Release r : releases) {
            int patchCount = r.getPatches() == null ? 0 : r.getPatches().size();
            System.out.printf("  %-15s  %-30s  [%d patch(es)]%n",
                    r.getVersion(), r.getDescription(), patchCount);
        }
    }

    private void handleDelete(List<String> args) {
        if (args.isEmpty()) {
            System.err.println("Usage: release delete <version>");
            return;
        }
        String version = args.get(0);
        Release release = Release.find(version);
        if (release == null) {
            System.err.println("Release '" + version + "' not found.");
            return;
        }
        if (release.delete()) {
            System.out.println("Release '" + version + "' deleted.");
        }
    }

    private void handleInfo(List<String> args) {
        if (args.isEmpty()) {
            System.err.println("Usage: release info <version>");
            return;
        }
        String version = args.get(0);
        Release release = Release.find(version);
        if (release == null) {
            System.err.println("Release '" + version + "' not found.");
            return;
        }

        System.out.println("=== Release: " + release.getVersion() + " ===");
        System.out.println("  Description : " + release.getDescription());
        System.out.println("  Created At  : " + release.getCreatedAt());
        System.out.println("  Directory   : " + release.getReleaseDir().getAbsolutePath());
        System.out.println("  Patches (" + release.getSortedPatches().size() + "):");

        if (release.getSortedPatches().isEmpty()) {
            System.out.println("    (none)");
        } else {
            for (Patch p : release.getSortedPatches()) {
                System.out.printf("    [%03d] %s  -  %s%n", p.getOrder(), p.getId(), p.getDescription());
            }
        }
    }

    private void printUsageHint() {
        System.out.println("Usage:");
        System.out.println("  release create <version> [description]");
        System.out.println("  release list");
        System.out.println("  release info <version>");
        System.out.println("  release delete <version>");
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Override public String getName()        { return "release"; }
    @Override public String getDescription() { return "Manage releases (create, list, info, delete)."; }
    @Override public String getUsage()       { return "release <create|list|info|delete> [args...]"; }
    @Override public String getHelp()        { return "Manages SQL release directories and their metadata."; }
    @Override public String getVersion()     { return "1.0"; }
    @Override public String getAuthor()      { return "Serban Tudor"; }
    @Override public String getDate()        { return "2026-04-01"; }
    @Override public String getLicense()     { return "MIT"; }
}