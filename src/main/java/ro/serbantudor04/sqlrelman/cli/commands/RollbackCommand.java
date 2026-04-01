package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.engine.DatabaseManager;
import ro.serbantudor04.sqlrelman.engine.MigrationEngine;
import ro.serbantudor04.sqlrelman.engine.models.Patch;
import ro.serbantudor04.sqlrelman.engine.models.Release;

import java.sql.Connection;
import java.util.List;

/**
 * Handles: rollback <release> <patch-id|name>
 *
 * Runs the down.sql of the specified patch and removes it from the migrations table.
 * Also supports: rollback <release> --last  to roll back the most recently applied patch.
 */
@Command
public class RollbackCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        if (args.size() < 2) {
            System.err.println("Usage: rollback <release> <patch-id|name|--last>");
            return;
        }

        String version = args.get(0);
        String patchArg = args.get(1);

        Release release = Release.find(version);
        if (release == null) {
            System.err.println("Release '" + version + "' not found.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            MigrationEngine engine = new MigrationEngine();
            engine.initMigrationTable(conn);
            List<String> applied = engine.getAppliedMigrations(conn);

            Patch targetPatch;

            if ("--last".equals(patchArg)) {
                // Find the last applied patch from this release
                targetPatch = release.getSortedPatches().reversed().stream()
                        .filter(p -> applied.contains(version + "/" + p.getId()))
                        .findFirst()
                        .orElse(null);

                if (targetPatch == null) {
                    System.out.println("No applied patches found for release '" + version + "'.");
                    return;
                }
            } else {
                targetPatch = release.getPatches().stream()
                        .filter(p -> p.getId().equals(patchArg) || p.getName().equals(patchArg))
                        .findFirst()
                        .orElse(null);

                if (targetPatch == null) {
                    System.err.println("Patch '" + patchArg + "' not found in release '" + version + "'.");
                    return;
                }
            }

            String migrationId = version + "/" + targetPatch.getId();
            if (!applied.contains(migrationId)) {
                System.err.println("Patch '" + targetPatch.getId() + "' has not been applied — nothing to roll back.");
                return;
            }

            System.out.print("Rolling back " + targetPatch.getId() + " ... ");
            engine.executeScript(conn, targetPatch.getDownSql(release.getReleaseDir()));
            engine.removeMigrationRecord(conn, migrationId);
            System.out.println("OK");

        } catch (Exception e) {
            System.err.println("Rollback failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override public String getName()        { return "rollback"; }
    @Override public String getDescription() { return "Roll back a patch by running its down.sql script."; }
    @Override public String getUsage()       { return "rollback <release> <patch-id|name|--last>"; }
    @Override public String getHelp()        { return "Executes the down.sql of a patch and removes its migration record."; }
    @Override public String getVersion()     { return "1.0"; }
    @Override public String getAuthor()      { return "Serban Tudor"; }
    @Override public String getDate()        { return "2026-04-01"; }
    @Override public String getLicense()     { return "MIT"; }
}