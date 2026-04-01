package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.engine.models.Patch;
import ro.serbantudor04.sqlrelman.engine.models.Release;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Handles: patch create <release> <name> [description]
 *          patch list <release>
 *          patch delete <release> <patch-id|name>
 *          patch show <release> <patch-id|name> [up|down]
 */
@Command
public class PatchCommand extends BaseCommand {

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
            case "list"   -> handleList(rest);
            case "delete" -> handleDelete(rest);
            case "show"   -> handleShow(rest);
            default -> {
                System.err.println("Unknown patch subcommand: " + subcommand);
                printUsageHint();
            }
        }
    }

    // -------------------------------------------------------------------------

    private void handleCreate(List<String> args) {
        if (args.size() < 2) {
            System.err.println("Usage: patch create <release> <name> [description]");
            return;
        }

        String version = args.get(0);
        String name = args.get(1);
        String description = args.size() > 2
                ? String.join(" ", args.subList(2, args.size()))
                : name;

        Release release = Release.find(version);
        if (release == null) {
            System.err.println("Release '" + version + "' not found. Create it first with: release create " + version);
            return;
        }

        Patch created = release.addPatch(name, description);
        if (created != null) {
            System.out.println("Added patch [" + created.getOrder() + "] '" + created.getId() + "' to release '" + version + "'.");
        }
    }

    private void handleList(List<String> args) {
        if (args.isEmpty()) {
            System.err.println("Usage: patch list <release>");
            return;
        }

        String version = args.get(0);
        Release release = Release.find(version);
        if (release == null) {
            System.err.println("Release '" + version + "' not found.");
            return;
        }

        List<Patch> patches = release.getSortedPatches();
        if (patches.isEmpty()) {
            System.out.println("No patches in release '" + version + "'.");
            return;
        }

        System.out.println("=== Patches for release: " + version + " ===");
        for (Patch p : patches) {
            System.out.printf("  [%03d] %-40s  %s%n", p.getOrder(), p.getId(), p.getDescription());
        }
    }

    private void handleDelete(List<String> args) {
        if (args.size() < 2) {
            System.err.println("Usage: patch delete <release> <patch-id|name>");
            return;
        }

        String version = args.get(0);
        String patchId = args.get(1);

        Release release = Release.find(version);
        if (release == null) {
            System.err.println("Release '" + version + "' not found.");
            return;
        }

        release.removePatch(patchId);
    }

    private void handleShow(List<String> args) {
        if (args.size() < 2) {
            System.err.println("Usage: patch show <release> <patch-id|name> [up|down]");
            return;
        }

        String version = args.get(0);
        String patchId = args.get(1);
        String direction = args.size() > 2 ? args.get(2).toLowerCase() : "up";

        Release release = Release.find(version);
        if (release == null) {
            System.err.println("Release '" + version + "' not found.");
            return;
        }

        Patch patch = release.getPatches().stream()
                .filter(p -> p.getId().equals(patchId) || p.getName().equals(patchId))
                .findFirst()
                .orElse(null);

        if (patch == null) {
            System.err.println("Patch '" + patchId + "' not found in release '" + version + "'.");
            return;
        }

        File sqlFile = direction.equals("down")
                ? patch.getDownSql(release.getReleaseDir())
                : patch.getUpSql(release.getReleaseDir());

        if (!sqlFile.exists()) {
            System.err.println("SQL file not found: " + sqlFile.getAbsolutePath());
            return;
        }

        try {
            System.out.println("--- " + direction.toUpperCase() + ".sql for patch: " + patch.getId() + " ---");
            System.out.println(Files.readString(sqlFile.toPath()));
        } catch (IOException e) {
            System.err.println("Failed to read SQL file: " + e.getMessage());
        }
    }

    private void printUsageHint() {
        System.out.println("Usage:");
        System.out.println("  patch create <release> <name> [description]");
        System.out.println("  patch list <release>");
        System.out.println("  patch show <release> <patch-id|name> [up|down]");
        System.out.println("  patch delete <release> <patch-id|name>");
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Override public String getName()        { return "patch"; }
    @Override public String getDescription() { return "Manage patches within a release (create, list, show, delete)."; }
    @Override public String getUsage()       { return "patch <create|list|show|delete> [args...]"; }
    @Override public String getHelp()        { return "Manages SQL patches (up/down scripts) inside a release."; }
    @Override public String getVersion()     { return "1.0"; }
    @Override public String getAuthor()      { return "Serban Tudor"; }
    @Override public String getDate()        { return "2026-04-01"; }
    @Override public String getLicense()     { return "MIT"; }
}