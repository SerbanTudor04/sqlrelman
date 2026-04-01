package ro.serbantudor04.sqlrelman.cli;

import ro.serbantudor04.sqlrelman.cli.commands.BaseCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the current namespace the shell is operating in.
 *
 * Root context  →  prompt: "sqlrelman> "
 * Patch context →  prompt: "sqlrelman:patch> "
 *
 * In a sub-context, commands are dispatched by prepending the context name,
 * so typing "create 1.0.0 add_users" in patch context runs "patch create 1.0.0 add_users".
 */
public class ShellContext {

    /** Commands that can be entered to pop back to the parent context. */
    public static final List<String> POP_COMMANDS = List.of("exit", "back", "..", "cd ..");

    /** Commands that fully quit the shell from any context. */
    public static final List<String> QUIT_COMMANDS = List.of("quit", "q", ":q");

    private final String name;           // null = root
    private final ShellContext parent;
    private final BaseCommand command;   // the command that owns this context

    /** Root context constructor. */
    public ShellContext() {
        this.name    = null;
        this.parent  = null;
        this.command = null;
    }

    /** Sub-context constructor. */
    public ShellContext(String name, ShellContext parent, BaseCommand command) {
        this.name    = name;
        this.parent  = parent;
        this.command = command;
    }

    public boolean isRoot() {
        return name == null;
    }

    /** The prompt string for this context level. */
    public String prompt() {
        if (isRoot()) return "sqlrelman> ";
        return "sqlrelman:" + buildPath() + "> ";
    }

    /** Full dot-separated path from root, e.g. "patch" or "release". */
    public String buildPath() {
        if (isRoot()) return "";
        if (parent == null || parent.isRoot()) return name;
        return parent.buildPath() + ":" + name;
    }

    /**
     * Given a raw input line, returns the fully-qualified command line to dispatch.
     * In root context it's returned as-is.
     * In a sub-context "create 1.0.0 foo" becomes "patch create 1.0.0 foo".
     */
    public String qualify(String line) {
        if (isRoot()) return line;
        return name + " " + line;
    }

    /**
     * Returns tab-completion candidates appropriate for this context.
     * In root: all top-level command names.
     * In sub-context: the subcommands of the owning command.
     */
    public List<String> completionCandidates() {
        List<String> candidates = new ArrayList<>();
        if (isRoot()) {
            return candidates; // caller fills root candidates from registry
        }
        // Sub-context candidates come from the command's known subcommands
        if (command != null) {
            candidates.addAll(parseSubcommands(command.getUsage()));
        }
        candidates.addAll(POP_COMMANDS);
        candidates.addAll(QUIT_COMMANDS);
        return candidates;
    }

    /**
     * Parses subcommand names out of a usage string like
     * "patch <create|list|show|edit|delete> [args...]"
     */
    private List<String> parseSubcommands(String usage) {
        List<String> result = new ArrayList<>();
        int start = usage.indexOf('<');
        int end   = usage.indexOf('>');
        if (start == -1 || end == -1 || end <= start) return result;
        String inner = usage.substring(start + 1, end);
        for (String sub : inner.split("\\|")) {
            result.add(sub.trim());
        }
        return result;
    }

    public String getName()         { return name; }
    public ShellContext getParent() { return parent; }
    public BaseCommand getCommand() { return command; }

    /** True if the input is a pop-to-parent command (but not a full quit). */
    public static boolean isPopCommand(String line) {
        return POP_COMMANDS.contains(line.trim().toLowerCase());
    }

    /** True if the input should quit the shell entirely. */
    public static boolean isQuitCommand(String line) {
        return QUIT_COMMANDS.contains(line.trim().toLowerCase());
    }
}