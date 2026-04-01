package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.config.AppConfig;
import ro.serbantudor04.sqlrelman.engine.DatabaseManager;
import ro.serbantudor04.sqlrelman.engine.MigrationEngine;

import java.io.File;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

@Command
public class MigrateCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        File releaseDir = new File(AppConfig.getInstance().releaseDirectory);
        if (!releaseDir.exists() || !releaseDir.isDirectory()) {
            System.out.println("Release directory not found. Create a patch first.");
            return;
        }

        File[] patchDirs = releaseDir.listFiles(File::isDirectory);
        if (patchDirs == null || patchDirs.length == 0) {
            System.out.println("No patches found to migrate.");
            return;
        }

        // Sort alphabetically (which sorts by timestamp due to our naming convention)
        Arrays.sort(patchDirs);

        MigrationEngine engine = new MigrationEngine();

        try (Connection conn = DatabaseManager.getConnection()) {
            engine.initMigrationTable(conn);
            List<String> appliedMigrations = engine.getAppliedMigrations(conn);

            int appliedCount = 0;

            for (File patchDir : patchDirs) {
                String patchId = patchDir.getName();

                if (appliedMigrations.contains(patchId)) {
                    continue; // Skip already applied
                }

                File upScript = new File(patchDir, "up.sql");
                System.out.print("Applying patch: " + patchId + "... ");

                conn.setAutoCommit(false); // Start transaction
                try {
                    engine.executeScript(conn, upScript);
                    engine.recordMigration(conn, patchId);
                    conn.commit();
                    System.out.println("SUCCESS");
                    appliedCount++;
                } catch (Exception e) {
                    conn.rollback();
                    System.out.println("FAILED");
                    System.err.println(e.getMessage());
                    break; // Stop execution on first failure
                } finally {
                    conn.setAutoCommit(true);
                }
            }

            if (appliedCount == 0) {
                System.out.println("Database is already up to date.");
            } else {
                System.out.println("Successfully applied " + appliedCount + " patches.");
            }

        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
        }
    }

    @Override
    public String getHelp() { return "Applies all pending up.sql scripts to the database."; }
    @Override
    public String getName() { return "migrate"; }
    @Override
    public String getUsage() { return "migrate"; }
    @Override
    public String getDescription() { return "Run pending migrations."; }
    @Override
    public String getVersion() { return "1.0"; }
    @Override
    public String getAuthor() { return "Serban Tudor"; }
    @Override
    public String getDate() { return "2026-03-31"; }
    @Override
    public String getLicense() { return "MIT"; }
}