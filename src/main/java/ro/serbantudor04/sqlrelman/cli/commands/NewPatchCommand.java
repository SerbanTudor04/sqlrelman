package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.config.AppConfig;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Command
public class NewPatchCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        if (args.isEmpty()) {
            System.out.println("Error: Provide a name for the patch. Usage: new_release <name>");
            return;
        }

        String patchName = String.join("_", args).toLowerCase().replaceAll("[^a-z0-9_]", "");
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String folderName = timestamp + "_" + patchName;

        File releaseDir = new File(AppConfig.getInstance().releaseDirectory);
        if (!releaseDir.exists() && !releaseDir.mkdirs()) {
            System.err.println("Failed to create release directory: " + releaseDir.getAbsolutePath());
            return;
        }

        File patchDir = new File(releaseDir, folderName);
        if (!patchDir.mkdirs()) {
            System.err.println("Failed to create patch directory: " + patchDir.getAbsolutePath());
            return;
        }

        try {
            File upFile = new File(patchDir, "up.sql");
            File downFile = new File(patchDir, "down.sql");

            Files.writeString(upFile.toPath(), "-- Write your UP migration SQL here\n");
            Files.writeString(downFile.toPath(), "-- Write your DOWN rollback SQL here\n");

            System.out.println("Created new patch: " + folderName);
            System.out.println("  -> " + upFile.getAbsolutePath());
            System.out.println("  -> " + downFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to create SQL files: " + e.getMessage());
        }
    }

    @Override
    public String getHelp() { return "Generates a new empty patch folder with up.sql and down.sql."; }
    @Override
    public String getName() { return "new_release"; }
    @Override
    public String getUsage() { return "new_release <patch_name>"; }
    @Override
    public String getDescription() { return "Creates a new SQL patch."; }
    @Override
    public String getVersion() { return "1.0"; }
    @Override
    public String getAuthor() { return "Serban Tudor"; }
    @Override
    public String getDate() { return "2026-03-31"; }
    @Override
    public String getLicense() { return "MIT"; }
}