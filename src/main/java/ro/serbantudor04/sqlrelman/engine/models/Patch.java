package ro.serbantudor04.sqlrelman.engine.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

public class Patch {

    private String id;         // e.g. "001_add_users_table"
    private String name;       // e.g. "add_users_table"
    private String description;
    private int order;
    private Date createdAt;

    // Jackson needs no-arg constructor
    public Patch() {}

    public Patch(String name, String description, int order) {
        this.name = normalizeName(name);
        this.description = description;
        this.order = order;
        this.createdAt = new Date();
        this.id = String.format("%03d_%s", order, this.name);
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    /**
     * Creates the patch directory and empty up/down SQL files inside the release directory.
     */
    public boolean create(File releaseDir) {
        File patchDir = getPatchDir(releaseDir);
        if (!patchDir.mkdirs()) {
            System.err.println("Failed to create patch directory: " + patchDir.getAbsolutePath());
            return false;
        }

        try {
            File upFile = new File(patchDir, "up.sql");
            File downFile = new File(patchDir, "down.sql");

            Files.writeString(upFile.toPath(), "-- UP migration for patch: " + id + "\n-- " + description + "\n\n");
            Files.writeString(downFile.toPath(), "-- DOWN migration (rollback) for patch: " + id + "\n\n");

            System.out.println("Patch '" + id + "' created.");
            System.out.println("  up.sql   -> " + upFile.getAbsolutePath());
            System.out.println("  down.sql -> " + downFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("Failed to write SQL files: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the patch directory from disk.
     */
    public boolean delete(File releaseDir) {
        File patchDir = getPatchDir(releaseDir);
        if (!patchDir.exists()) {
            System.err.println("Patch directory not found: " + patchDir.getAbsolutePath());
            return false;
        }
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(patchDir);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to delete patch directory: " + e.getMessage());
            return false;
        }
    }

    @JsonIgnore
    public File getPatchDir(File releaseDir) {
        return new File(releaseDir, id);
    }

    @JsonIgnore
    public File getUpSql(File releaseDir) {
        return new File(getPatchDir(releaseDir), "up.sql");
    }

    @JsonIgnore
    public File getDownSql(File releaseDir) {
        return new File(getPatchDir(releaseDir), "down.sql");
    }

    // --- Getters & Setters for Jackson ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}