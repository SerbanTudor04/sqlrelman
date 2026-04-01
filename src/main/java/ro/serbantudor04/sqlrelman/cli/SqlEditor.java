package ro.serbantudor04.sqlrelman.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A lightweight interactive SQL editor that runs entirely in the terminal —
 * no external process, no TERM variable required.
 *
 * Commands (typed on a blank line starting with ':'):
 *   :w   / :save     — save and stay
 *   :wq  / :x        — save and exit
 *   :q   / :quit     — exit without saving (prompts if unsaved changes)
 *   :q!              — force quit, discard changes
 *   :show / :p       — print current buffer with line numbers
 *   :d <n>           — delete line n
 *   :d <n>-<m>       — delete lines n through m
 *   :i <n>           — insert before line n (then type content, '.' to end)
 *   :undo            — undo last destructive operation
 *   :clear           — wipe entire buffer
 *   :help / :?       — print this help
 */
public class SqlEditor {

    private final File file;
    private final List<String> buffer = new ArrayList<>();
    private List<String> undoSnapshot = null;  // one level of undo
    private boolean dirty = false;

    public SqlEditor(File file) {
        this.file = file;
    }

    /**
     * Opens the editor. Returns true if the file was saved, false if discarded.
     */
    public boolean open() {
        loadFile();
        printHeader();
        printBuffer();
        printHint();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print(dirty ? "  sql*> " : "  sql>  ");
            System.out.flush();

            if (!scanner.hasNextLine()) {
                // EOF (Ctrl+D) — save automatically
                save();
                return true;
            }

            String raw = scanner.nextLine();

            if (raw.startsWith(":")) {
                EditorAction action = handleCommand(raw.trim(), scanner);
                if (action == EditorAction.SAVED_EXIT)    return true;
                if (action == EditorAction.DISCARDED_EXIT) return false;
                // CONTINUE — stay in the loop
            } else {
                // Regular input: append line to buffer
                saveUndoSnapshot();
                buffer.add(raw);
                dirty = true;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Command handling
    // -------------------------------------------------------------------------

    private enum EditorAction { CONTINUE, SAVED_EXIT, DISCARDED_EXIT }

    private EditorAction handleCommand(String cmd, Scanner scanner) {
        String lower = cmd.toLowerCase();

        switch (lower) {
            case ":w", ":save" -> { save(); return EditorAction.CONTINUE; }

            case ":wq", ":x"   -> { save(); return EditorAction.SAVED_EXIT; }

            case ":q", ":quit" -> {
                if (dirty) {
                    System.out.println("  Unsaved changes. Use ':wq' to save and quit, or ':q!' to discard.");
                    return EditorAction.CONTINUE;
                }
                return EditorAction.DISCARDED_EXIT;
            }

            case ":q!" -> {
                System.out.println("  Discarded changes.");
                return EditorAction.DISCARDED_EXIT;
            }

            case ":show", ":p" -> { printBuffer(); return EditorAction.CONTINUE; }

            case ":undo" -> {
                if (undoSnapshot == null) {
                    System.out.println("  Nothing to undo.");
                } else {
                    buffer.clear();
                    buffer.addAll(undoSnapshot);
                    undoSnapshot = null;
                    dirty = true;
                    System.out.println("  Undo applied.");
                    printBuffer();
                }
                return EditorAction.CONTINUE;
            }

            case ":clear" -> {
                saveUndoSnapshot();
                buffer.clear();
                dirty = true;
                System.out.println("  Buffer cleared.");
                return EditorAction.CONTINUE;
            }

            case ":help", ":?" -> { printHelp(); return EditorAction.CONTINUE; }

            default -> {
                // :d <n>  or  :d <n>-<m>
                if (lower.startsWith(":d ")) {
                    handleDelete(cmd.substring(3).trim());
                    return EditorAction.CONTINUE;
                }
                // :i <n>
                if (lower.startsWith(":i ")) {
                    handleInsert(cmd.substring(3).trim(), scanner);
                    return EditorAction.CONTINUE;
                }
                System.out.println("  Unknown command '" + cmd + "'. Type ':help' for commands.");
                return EditorAction.CONTINUE;
            }
        }
    }

    private void handleDelete(String arg) {
        try {
            if (arg.contains("-")) {
                String[] parts = arg.split("-");
                int from = Integer.parseInt(parts[0].trim()) - 1;
                int to   = Integer.parseInt(parts[1].trim()) - 1;
                if (!validRange(from, to)) return;
                saveUndoSnapshot();
                buffer.subList(from, to + 1).clear();
                dirty = true;
                System.out.println("  Deleted lines " + (from + 1) + "-" + (to + 1) + ".");
            } else {
                int n = Integer.parseInt(arg.trim()) - 1;
                if (!validLine(n)) return;
                saveUndoSnapshot();
                buffer.remove(n);
                dirty = true;
                System.out.println("  Deleted line " + (n + 1) + ".");
            }
            printBuffer();
        } catch (NumberFormatException e) {
            System.out.println("  Usage: :d <line>  or  :d <from>-<to>");
        }
    }

    private void handleInsert(String arg, Scanner scanner) {
        int n;
        try {
            n = Integer.parseInt(arg.trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("  Usage: :i <line number>");
            return;
        }
        if (n < 0 || n > buffer.size()) {
            System.out.println("  Line " + (n + 1) + " is out of range (1-" + (buffer.size() + 1) + ").");
            return;
        }
        System.out.println("  Inserting before line " + (n + 1) + ". Type '.' on its own line to finish.");
        saveUndoSnapshot();
        int insertAt = n;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equals(".")) break;
            buffer.add(insertAt++, line);
            dirty = true;
        }
        printBuffer();
    }

    // -------------------------------------------------------------------------
    // File I/O
    // -------------------------------------------------------------------------

    private void loadFile() {
        buffer.clear();
        if (file.exists()) {
            try {
                String content = Files.readString(file.toPath());
                if (!content.isBlank()) {
                    for (String line : content.split("\n", -1)) {
                        buffer.add(line);
                    }
                    // Remove trailing empty line from split
                    if (!buffer.isEmpty() && buffer.getLast().isEmpty()) {
                        buffer.removeLast();
                    }
                }
            } catch (IOException e) {
                System.err.println("  Warning: could not read file: " + e.getMessage());
            }
        }
    }

    private void save() {
        try {
            Files.writeString(file.toPath(), String.join("\n", buffer) + (buffer.isEmpty() ? "" : "\n"));
            dirty = false;
            System.out.println("  Saved → " + file.getName());
        } catch (IOException e) {
            System.err.println("  Failed to save: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    private void printHeader() {
        System.out.println();
        System.out.println("  ╔══ " + file.getName() + " " + "═".repeat(Math.max(0, 52 - file.getName().length())) + "╗");
        System.out.printf("  ║  %d line(s)  │  %s%n",
                buffer.size(), file.getAbsolutePath());
        System.out.println("  ╚" + "═".repeat(56) + "╝");
    }

    private void printBuffer() {
        if (buffer.isEmpty()) {
            System.out.println("  (empty)");
            return;
        }
        System.out.println();
        for (int i = 0; i < buffer.size(); i++) {
            System.out.printf("  %3d │ %s%n", i + 1, buffer.get(i));
        }
        System.out.println();
    }

    private void printHint() {
        System.out.println("  Type SQL to append lines. Commands start with ':'");
        System.out.println("  :show  :d <n>  :i <n>  :undo  :w  :wq  :q  :help");
        System.out.println();
    }

    private void printHelp() {
        System.out.println();
        System.out.println("  ─── SqlEditor commands ─────────────────────────────");
        System.out.println("  :show / :p        print buffer with line numbers");
        System.out.println("  :d <n>            delete line n");
        System.out.println("  :d <n>-<m>        delete lines n through m");
        System.out.println("  :i <n>            insert lines before line n");
        System.out.println("  :undo             undo last delete/insert/clear");
        System.out.println("  :clear            wipe entire buffer");
        System.out.println("  :w  / :save       save file, keep editing");
        System.out.println("  :wq / :x          save and exit");
        System.out.println("  :q  / :quit       exit (warns if unsaved)");
        System.out.println("  :q!               exit and discard changes");
        System.out.println("  ────────────────────────────────────────────────────");
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Guards
    // -------------------------------------------------------------------------

    private void saveUndoSnapshot() {
        undoSnapshot = new ArrayList<>(buffer);
    }

    private boolean validLine(int idx) {
        if (idx < 0 || idx >= buffer.size()) {
            System.out.println("  Line " + (idx + 1) + " is out of range (1-" + buffer.size() + ").");
            return false;
        }
        return true;
    }

    private boolean validRange(int from, int to) {
        if (from < 0 || to >= buffer.size() || from > to) {
            System.out.println("  Invalid range " + (from + 1) + "-" + (to + 1)
                    + " (buffer has " + buffer.size() + " lines).");
            return false;
        }
        return true;
    }
}