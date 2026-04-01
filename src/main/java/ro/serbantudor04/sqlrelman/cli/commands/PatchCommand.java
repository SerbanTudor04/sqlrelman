package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.SqlEditor;
import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.engine.models.Patch;
import ro.serbantudor04.sqlrelman.engine.models.Release;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Handles: patch create <release> <n> [description]
 *          patch list <release>
 *          patch show <release> <patch-id|name> [up|down]
 *          patch edit <release> <patch-id|name> [up|down|both]
 *          patch delete <release> <patch-id|name>
 */
@Command
public class PatchCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        if (args.isEmpty()) { printUsageHint(); return; }

        String subcommand = args.get(0).toLowerCase();
        List<String> rest = args.size() > 1 ? args.subList(1, args.size()) : List.of();

        switch (subcommand) {
            case "create" -> handleCreate(rest);
            case "list"   -> handleList(rest);
            case "delete" -> handleDelete(rest);
            case "show"   -> handleShow(rest);
            case "edit"   -> handleEdit(rest);
            default -> { System.err.println("Unknown patch subcommand: " + subcommand); printUsageHint(); }
        }
    }

    // -------------------------------------------------------------------------

    private void handleCreate(List<String> args) {
        if (args.size() < 2) { System.err.println("Usage: patch create <release> <n> [description]"); return; }

        String version     = args.get(0);
        String name        = args.get(1);
        String description = args.size() > 2 ? String.join(" ", args.subList(2, args.size())) : name;

        Release release = Release.find(version);

        // Auto-create the release if it doesn't exist yet
        if (release == null) {
            System.out.println("  Release '" + version + "' not found — creating it automatically.");
            release = new Release(version, "Release " + version);
            if (!release.create()) return;
            System.out.println("  Release '" + version + "' created.");
        }

        Patch created = release.addPatch(name, description);
        if (created != null) {
            System.out.println("Added patch [" + created.getOrder() + "] '" + created.getId() + "' to release '" + version + "'.");
        }
    }

    private void handleList(List<String> args) {
        if (args.isEmpty()) { System.err.println("Usage: patch list <release>"); return; }
        String version = args.get(0);
        Release release = Release.find(version);
        if (release == null) { System.err.println("Release '" + version + "' not found."); return; }
        List<Patch> patches = release.getSortedPatches();
        if (patches.isEmpty()) { System.out.println("No patches in release '" + version + "'."); return; }
        System.out.println("=== Patches for release: " + version + " ===");
        for (Patch p : patches) {
            System.out.printf("  [%03d] %-40s  %s%n", p.getOrder(), p.getId(), p.getDescription());
        }
    }

    private void handleDelete(List<String> args) {
        if (args.size() < 2) { System.err.println("Usage: patch delete <release> <patch-id|name>"); return; }
        Release release = Release.find(args.get(0));
        if (release == null) { System.err.println("Release '" + args.get(0) + "' not found."); return; }
        release.removePatch(args.get(1));
    }

    private void handleShow(List<String> args) {
        if (args.size() < 2) { System.err.println("Usage: patch show <release> <patch-id|name> [up|down]"); return; }
        String version   = args.get(0);
        String patchId   = args.get(1);
        String direction = args.size() > 2 ? args.get(2).toLowerCase() : "up";
        Release release = Release.find(version);
        if (release == null) { System.err.println("Release '" + version + "' not found."); return; }
        Patch patch = findPatch(release, patchId);
        if (patch == null) { System.err.println("Patch '" + patchId + "' not found."); return; }
        File sqlFile = direction.equals("down") ? patch.getDownSql(release.getReleaseDir()) : patch.getUpSql(release.getReleaseDir());
        if (!sqlFile.exists()) { System.err.println("SQL file not found: " + sqlFile.getAbsolutePath()); return; }
        try {
            System.out.println("--- " + direction.toUpperCase() + ".sql for patch: " + patch.getId() + " ---");
            System.out.println(Files.readString(sqlFile.toPath()));
        } catch (IOException e) {
            System.err.println("Failed to read SQL file: " + e.getMessage());
        }
    }

    private void handleEdit(List<String> args) {
        if (args.size() < 2) { System.err.println("Usage: patch edit <release> <patch-id|name> [up|down|both]"); return; }

        String version   = args.get(0);
        String patchId   = args.get(1);
        String preselect = args.size() > 2 ? args.get(2).toLowerCase() : null;

        Release release = Release.find(version);
        if (release == null) { System.err.println("Release '" + version + "' not found."); return; }

        Patch patch = findPatch(release, patchId);
        if (patch == null) { System.err.println("Patch '" + patchId + "' not found in release '" + version + "'."); return; }

        File upFile   = patch.getUpSql(release.getReleaseDir());
        File downFile = patch.getDownSql(release.getReleaseDir());

        List<File> toEdit = new ArrayList<>();
        if (preselect != null) {
            switch (preselect) {
                case "up"   -> toEdit.add(upFile);
                case "down" -> toEdit.add(downFile);
                case "both" -> { toEdit.add(upFile); toEdit.add(downFile); }
                default -> { System.err.println("Unknown target '" + preselect + "'. Use: up, down, or both."); return; }
            }
        } else {
            File selected = promptFileSelection(upFile, downFile);
            if (selected == null) return;
            toEdit.add(selected);
        }

        for (File file : toEdit) {
            editFile(file);
        }
    }

    // -------------------------------------------------------------------------
    // Editor dispatch
    // -------------------------------------------------------------------------

    /** Numbered menu: 1=up.sql  2=down.sql  0=cancel */
    private File promptFileSelection(File upFile, File downFile) {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("  Which file do you want to edit?");
        System.out.println("    1. up.sql   (apply migration)");
        System.out.println("    2. down.sql (rollback migration)");
        System.out.println("    0. Cancel");
        System.out.print("  Choice [1/2/0]: ");
        return switch (scanner.nextLine().trim()) {
            case "1"  -> upFile;
            case "2"  -> downFile;
            default   -> { System.out.println("  Cancelled."); yield null; }
        };
    }

    /**
     * Tries to open the file in a system editor ($EDITOR / $VISUAL / nano / vim / vi).
     * If the editor fails or is unavailable, falls back to the built-in SqlEditor.
     */
    private void editFile(File file) {
        String editor = resolveEditor();
        if (editor != null && tryOpenInEditor(editor, file)) return;
        // System editor unavailable or failed — use built-in SqlEditor
        new SqlEditor(file).open();
    }

    /** $EDITOR -> $VISUAL -> nano -> vim -> vi -> null */
    private String resolveEditor() {
        for (String env : new String[]{"EDITOR", "VISUAL"}) {
            String val = System.getenv(env);
            if (val != null && !val.isBlank()) return val;
        }
        for (String candidate : new String[]{"nano", "vim", "vi"}) {
            if (commandExists(candidate)) return candidate;
        }
        return null;
    }

    private boolean commandExists(String cmd) {
        try {
            return new ProcessBuilder("which", cmd).redirectErrorStream(true).start().waitFor() == 0;
        } catch (Exception e) { return false; }
    }

    /**
     * Launches the system editor. Returns true on success (exit code 0),
     * false if the process fails — triggering the SqlEditor fallback.
     */
    private boolean tryOpenInEditor(String editor, File file) {
        System.out.println("  Opening " + file.getName() + " in " + editor + " ...");
        try {
            int code = new ProcessBuilder(editor, file.getAbsolutePath())
                    .inheritIO()
                    .start()
                    .waitFor();
            if (code == 0) {
                System.out.println("  Saved: " + file.getAbsolutePath());
                return true;
            }
            System.out.println("  Editor exited with code " + code + " — falling back to built-in editor.");
            return false;
        } catch (IOException | InterruptedException e) {
            System.out.println("  Could not launch editor: " + e.getMessage() + " — using built-in editor.");
            return false;
        }
    }

    // -------------------------------------------------------------------------

    private Patch findPatch(Release release, String patchId) {
        return release.getPatches().stream()
                .filter(p -> p.getId().equals(patchId) || p.getName().equals(patchId))
                .findFirst().orElse(null);
    }

    private void printUsageHint() {
        System.out.println("Usage:");
        System.out.println("  patch create <release> <n> [description]");
        System.out.println("  patch list <release>");
        System.out.println("  patch show <release> <patch-id|name> [up|down]");
        System.out.println("  patch edit <release> <patch-id|name> [up|down|both]");
        System.out.println("  patch delete <release> <patch-id|name>");
    }

    @Override public String getName()        { return "patch"; }
    @Override public String getDescription() { return "Manage patches within a release (create, list, show, edit, delete)."; }
    @Override public String getUsage()       { return "patch <create|list|show|edit|delete> [args...]"; }
    @Override public String getHelp()        { return "Manages SQL patches (up/down scripts) inside a release."; }
    @Override public String getVersion()     { return "1.0"; }
    @Override public String getAuthor()      { return "Serban Tudor"; }
    @Override public String getDate()        { return "2026-04-01"; }
    @Override public String getLicense()     { return "MIT"; }
}