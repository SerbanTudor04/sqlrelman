package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.engine.DatabaseManager;
import ro.serbantudor04.sqlrelman.engine.MigrationEngine;
import ro.serbantudor04.sqlrelman.engine.models.Patch;
import ro.serbantudor04.sqlrelman.engine.models.Release;

import java.sql.Connection;
import java.util.List;

/**
 * Handles: migrate <release>
 *
 * Runs all unapplied patches in the given release, in order.
 * Tracks applied patches in the sqlrelman_migrations table.
 */
@Command
public class MigrateCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        if (args.isEmpty()) {
            System.err.println("Usage: migrate <release>");
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
            System.out.println("Release '" + version + "' has no patches to migrate.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            MigrationEngine engine = new MigrationEngine();
            engine.initMigrationTable(conn);

            List<String> applied = engine.getAppliedMigrations(conn);

            System.out.println("=== Migrating release: " + version + " ===");
            int skipped = 0;
            int executed = 0;

            for (Patch patch : patches) {
                String migrationId = version + "/" + patch.getId();

                if (applied.contains(migrationId)) {
                    System.out.println("  [SKIP]  " + patch.getId() + " (already applied)");
                    skipped++;
                    continue;
                }

                System.out.print("  [RUN ]  " + patch.getId() + " ... ");
                engine.executeScript(conn, patch.getUpSql(release.getReleaseDir()));
                engine.recordMigration(conn, migrationId);
                System.out.println("OK");
                executed++;
            }

            System.out.println("=== Done: " + executed + " applied, " + skipped + " skipped ===");

        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override public String getName()        { return "migrate"; }
    @Override public String getDescription() { return "Apply all pending patches in a release to the database."; }
    @Override public String getUsage()       { return "migrate <release>"; }
    @Override public String getHelp()        { return "Runs all unapplied up.sql scripts in a release, in order."; }
    @Override public String getVersion()     { return "1.0"; }
    @Override public String getAuthor()      { return "Serban Tudor"; }
    @Override public String getDate()        { return "2026-04-01"; }
    @Override public String getLicense()     { return "MIT"; }
}