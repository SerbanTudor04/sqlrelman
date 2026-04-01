package ro.serbantudor04.sqlrelman.cli;

import ro.serbantudor04.sqlrelman.cli.commands.BaseCommand;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interactive REPL shell with context/namespace support.
 *
 * Root:           sqlrelman>
 * In patch ctx:   sqlrelman:patch>      (type subcommands without the "patch" prefix)
 * In release ctx: sqlrelman:release>
 *
 * Navigation:
 *   patch          → enter patch context
 *   release        → enter release context
 *   exit / back / ..  → pop back to parent context
 *   quit / q / :q     → exit the shell entirely from any depth
 */
public class InteractiveShell {

    private static final String VERSION = "1.0";

    /** Commands that enter a sub-context rather than running directly. */
    private static final Set<String> CONTEXTUAL_COMMANDS = Set.of("patch", "release", "driver");

    private final CLI cli;

    // Current context — mutated as the user navigates in/out of namespaces
    private ShellContext context = new ShellContext();

    public InteractiveShell(CLI cli) {
        this.cli = cli;
    }

    public void start() {
        printWelcome();
        if (isJLineAvailable() && isRealTerminal()) {
            silenceJLineLogger();
            startJLine();
        } else {
            startPlain();
        }
    }

    // -------------------------------------------------------------------------
    // JLine3 rich path
    // -------------------------------------------------------------------------

    private void startJLine() {
        try {
            Class<?> termBuilderClass   = Class.forName("org.jline.terminal.TerminalBuilder");
            Class<?> readerBuilderClass = Class.forName("org.jline.reader.LineReaderBuilder");
            Class<?> terminalClass      = Class.forName("org.jline.terminal.Terminal");
            Class<?> historyClass       = Class.forName("org.jline.reader.impl.history.DefaultHistory");
            Class<?> eofClass           = Class.forName("org.jline.reader.EndOfFileException");
            Class<?> interruptClass     = Class.forName("org.jline.reader.UserInterruptException");

            Object termBuilder = termBuilderClass.getMethod("builder").invoke(null);
            termBuilder = termBuilderClass.getMethod("system", boolean.class).invoke(termBuilder, true);
            Object terminal = termBuilderClass.getMethod("build").invoke(termBuilder);

            Object history = historyClass.getDeclaredConstructor().newInstance();

            Object readerBuilder = readerBuilderClass.getMethod("builder").invoke(null);
            readerBuilder = readerBuilderClass.getMethod("terminal", terminalClass).invoke(readerBuilder, terminal);
            readerBuilder = readerBuilderClass.getMethod("history",
                    Class.forName("org.jline.reader.History")).invoke(readerBuilder, history);
            // Completer is set dynamically via updateCompleter each time context changes
            Object lineReader = readerBuilderClass.getMethod("build").invoke(readerBuilder);

            updateCompleter(lineReader);

            while (true) {
                String line;
                try {
                    line = (String) lineReader.getClass()
                            .getMethod("readLine", String.class)
                            .invoke(lineReader, context.prompt());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    if (eofClass.isInstance(cause)) {
                        // Ctrl+D: pop context if in sub-context, quit if at root
                        if (context.isRoot()) break;
                        popContext();
                        updateCompleter(lineReader);
                        continue;
                    }
                    if (interruptClass.isInstance(cause)) continue; // Ctrl+C clears line
                    throw e;
                }

                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;

                DispatchResult result = dispatch(line);
                if (result == DispatchResult.QUIT) break;
                if (result == DispatchResult.CONTEXT_CHANGED) updateCompleter(lineReader);
            }

            terminal.getClass().getMethod("close").invoke(terminal);

        } catch (Exception e) {
            startPlain();
            return;
        }

        printGoodbye();
    }

    /** Replaces the LineReader's completer with candidates for the current context. */
    private void updateCompleter(Object lineReader) throws Exception {
        List<String> candidates = buildCandidates();
        Class<?> stringsCompleter = Class.forName("org.jline.reader.impl.completer.StringsCompleter");
        Object completer = stringsCompleter.getConstructor(Iterable.class).newInstance(candidates);
        lineReader.getClass()
                .getMethod("setCompleter", Class.forName("org.jline.reader.Completer"))
                .invoke(lineReader, completer);
    }

    private List<String> buildCandidates() {
        List<String> candidates = new ArrayList<>();
        if (context.isRoot()) {
            for (BaseCommand cmd : cli.getLoadedCommands()) {
                candidates.add(cmd.getName());
            }
            candidates.addAll(ShellContext.QUIT_COMMANDS);
        } else {
            candidates.addAll(context.completionCandidates());
        }
        return candidates;
    }

    // -------------------------------------------------------------------------
    // Plain fallback path
    // -------------------------------------------------------------------------

    private void startPlain() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(context.prompt());
            System.out.flush();
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            DispatchResult result = dispatch(line);
            if (result == DispatchResult.QUIT) break;
        }
        printGoodbye();
    }

    // -------------------------------------------------------------------------
    // Dispatch — shared by both paths
    // -------------------------------------------------------------------------

    private enum DispatchResult { OK, QUIT, CONTEXT_CHANGED }

    private DispatchResult dispatch(String line) {
        // Full quit from any context
        if (ShellContext.isQuitCommand(line)) return DispatchResult.QUIT;

        // Pop context: exit/back/.. goes up one level; at root it quits
        if (ShellContext.isPopCommand(line)) {
            if (context.isRoot()) return DispatchResult.QUIT;
            popContext();
            return DispatchResult.CONTEXT_CHANGED;
        }

        // Parse tokens
        String[] tokens = line.split("\\s+");
        List<String> parts = new ArrayList<>(Arrays.asList(tokens));

        // --help / -h handling
        if (parts.remove("--help") || parts.remove("-h")) {
            String cmdName = parts.isEmpty()
                    ? (context.isRoot() ? "help" : context.getName())
                    : parts.get(0).toLowerCase();
            BaseCommand cmd = cli.findCommand(context.isRoot() ? cmdName : context.getName());
            if (cmd != null) printCommandHelp(cmd);
            else             System.out.println("Unknown command: " + cmdName);
            return DispatchResult.OK;
        }

        if (parts.isEmpty()) return DispatchResult.OK;

        String first = parts.get(0).toLowerCase();

        // Context entry: typing "patch" alone (or "release", "driver") pushes context
        if (context.isRoot() && CONTEXTUAL_COMMANDS.contains(first) && parts.size() == 1) {
            BaseCommand cmd = cli.findCommand(first);
            if (cmd != null) {
                pushContext(first, cmd);
                return DispatchResult.CONTEXT_CHANGED;
            }
        }

        // In a sub-context, qualify the line by prepending the context name
        String qualified = context.qualify(line);
        String[] qTokens = qualified.split("\\s+");
        String commandName = qTokens[0].toLowerCase();
        List<String> args  = new ArrayList<>(Arrays.asList(qTokens).subList(1, qTokens.length));

        BaseCommand cmd = cli.findCommand(commandName);
        if (cmd != null) {
            try {
                cmd.run(args);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        } else {
            System.out.println("Unknown command: '" + first + "'. Type 'help' to list commands.");
        }

        return DispatchResult.OK;
    }

    // -------------------------------------------------------------------------
    // Context navigation
    // -------------------------------------------------------------------------

    private void pushContext(String name, BaseCommand cmd) {
        context = new ShellContext(name, context, cmd);
        System.out.println("  Entering " + name + " context. Type 'exit' or '..' to go back.");
        printContextHelp();
    }

    private void popContext() {
        String leaving = context.getName();
        context = context.getParent() != null ? context.getParent() : new ShellContext();
        System.out.println("  Left " + leaving + " context → " + context.prompt().trim());
    }

    private void printContextHelp() {
        BaseCommand cmd = context.getCommand();
        if (cmd == null) return;
        System.out.println("  Usage: " + cmd.getUsage());
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Terminal detection & JLine utilities
    // -------------------------------------------------------------------------

    private boolean isRealTerminal() {
        if (System.console() != null) return true;
        if ("dumb".equals(System.getenv("TERM"))) return false;
        if (System.getProperty("idea.launcher.bin.path") != null) return false;
        if (System.getProperty("idea.home.path") != null) return false;
        return false;
    }

    private void silenceJLineLogger() {
        Logger.getLogger("org.jline").setLevel(Level.OFF);
    }

    private boolean isJLineAvailable() {
        try {
            Class.forName("org.jline.terminal.TerminalBuilder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private void printWelcome() {
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │        SQLRelMan Interactive Shell       │");
        System.out.println("  │                  v" + VERSION + "                     │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println("  Type 'help' for available commands.");
        System.out.println("  Type 'patch' or 'release' to enter a context.");
        System.out.println("  Type 'exit' / '..' to leave a context, 'quit' to exit.");
        System.out.println();
    }

    private void printGoodbye() {
        System.out.println("\nBye!");
    }

    private void printCommandHelp(BaseCommand cmd) {
        System.out.println();
        System.out.println("  Command : " + cmd.getName());
        System.out.println("  Usage   : " + cmd.getUsage());
        System.out.println("  Desc    : " + cmd.getDescription());
        System.out.println("  Help    : " + cmd.getHelp());
        System.out.println();
    }
}