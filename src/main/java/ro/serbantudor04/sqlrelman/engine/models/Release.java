package ro.serbantudor04.sqlrelman.engine.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;
import ro.serbantudor04.sqlrelman.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Release {

    public static final String INFO_FILE = "info.json";

    private String version;
    private String description;
    private Date createdAt;
    private List<Patch> patches;

    // Jackson no-arg constructor
    public Release() {
        this.patches = new ArrayList<>();
    }

    public Release(String version, String description) {
        this.version = version;
        this.description = description;
        this.createdAt = new Date();
        this.patches = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Disk operations
    // -------------------------------------------------------------------------

    /**
     * Creates the release directory and its info.json on disk.
     */
    public boolean create() {
        File dir = getReleaseDir();
        if (dir.exists()) {
            System.err.println("Release '" + version + "' already exists.");
            return false;
        }
        if (!dir.mkdirs()) {
            System.err.println("Failed to create release directory: " + dir.getAbsolutePath());
            return false;
        }
        return saveInfoFile();
    }

    /**
     * Deletes the entire release directory from disk.
     */
    public boolean delete() {
        File dir = getReleaseDir();
        if (!dir.exists()) {
            System.err.println("Release '" + version + "' not found.");
            return false;
        }
        try {
            FileUtils.deleteDirectory(dir);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to delete release directory: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a new patch to this release, creates its directory, and saves info.json.
     */
    public Patch addPatch(String name, String patchDescription) {
        int nextOrder = patches.size() + 1;
        Patch patch = new Patch(name, patchDescription, nextOrder);

        File releaseDir = getReleaseDir();
        if (!patch.create(releaseDir)) {
            return null;
        }

        patches.add(patch);
        saveInfoFile();
        return patch;
    }

    /**
     * Removes a patch by ID, deletes its directory, re-numbers remaining patches, and saves.
     */
    public boolean removePatch(String patchId) {
        Patch toRemove = patches.stream()
                .filter(p -> p.getId().equals(patchId) || p.getName().equals(patchId))
                .findFirst()
                .orElse(null);

        if (toRemove == null) {
            System.err.println("Patch '" + patchId + "' not found in release '" + version + "'.");
            return false;
        }

        if (!toRemove.delete(getReleaseDir())) {
            return false;
        }

        patches.remove(toRemove);
        renumberPatches();
        saveInfoFile();
        System.out.println("Patch '" + patchId + "' deleted from release '" + version + "'.");
        return true;
    }

    /**
     * Re-sorts and re-numbers patches after a deletion to keep ordering consistent.
     */
    private void renumberPatches() {
        patches.sort(Comparator.comparingInt(Patch::getOrder));
        for (int i = 0; i < patches.size(); i++) {
            patches.get(i).setOrder(i + 1);
        }
    }

    /**
     * Persists this release's metadata (including all patch metadata) to info.json.
     */
    public boolean saveInfoFile() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            mapper.writeValue(new File(getReleaseDir(), INFO_FILE), this);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save info.json: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads a Release from an existing directory's info.json.
     */
    public static Release load(File releaseDir) {
        File infoFile = new File(releaseDir, INFO_FILE);
        if (!infoFile.exists()) {
            System.err.println("No info.json found in: " + releaseDir.getAbsolutePath());
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        try {
            return mapper.readValue(infoFile, Release.class);
        } catch (Exception e) {
            System.err.println("Failed to read info.json: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lists all releases found in the configured releases directory.
     */
    public static List<Release> listAll() {
        File releasesRoot = new File(AppConfig.getInstance().releasesDirectory);
        List<Release> releases = new ArrayList<>();

        if (!releasesRoot.exists() || !releasesRoot.isDirectory()) {
            return releases;
        }

        File[] dirs = releasesRoot.listFiles(File::isDirectory);
        if (dirs == null) return releases;

        for (File dir : dirs) {
            Release r = Release.load(dir);
            if (r != null) releases.add(r);
        }

        releases.sort(Comparator.comparing(Release::getVersion));
        return releases;
    }

    /**
     * Finds a release by version string.
     */
    public static Release find(String version) {
        File dir = new File(AppConfig.getInstance().releasesDirectory, version);
        if (!dir.exists()) return null;
        return Release.load(dir);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @JsonIgnore
    public File getReleaseDir() {
        return new File(AppConfig.getInstance().releasesDirectory, version);
    }

    @JsonIgnore
    public List<Patch> getSortedPatches() {
        List<Patch> sorted = new ArrayList<>(patches);
        sorted.sort(Comparator.comparingInt(Patch::getOrder));
        return sorted;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public List<Patch> getPatches() { return patches; }
    public void setPatches(List<Patch> patches) { this.patches = patches; }
}