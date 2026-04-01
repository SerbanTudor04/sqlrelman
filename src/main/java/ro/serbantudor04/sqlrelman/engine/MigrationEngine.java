package ro.serbantudor04.sqlrelman.engine;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MigrationEngine {

    private static final String TABLE_NAME = "sqlrelman_migrations";

    public void initMigrationTable(Connection conn) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id VARCHAR(255) PRIMARY KEY, " +
                "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public List<String> getAppliedMigrations(Connection conn) throws Exception {
        List<String> applied = new ArrayList<>();
        String sql = "SELECT id FROM " + TABLE_NAME + " ORDER BY id ASC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                applied.add(rs.getString("id"));
            }
        }
        return applied;
    }

    public void executeScript(Connection conn, File scriptFile) throws Exception {
        if (!scriptFile.exists()) {
            throw new Exception("Script file not found: " + scriptFile.getAbsolutePath());
        }

        String content = Files.readString(scriptFile.toPath());
        // Basic split by semicolon for multiple statements
        String[] statements = content.split(";");

        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql);
                }
            }
        }
    }

    public void recordMigration(Connection conn, String patchId) throws Exception {
        String sql = "INSERT INTO " + TABLE_NAME + " (id) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, patchId);
            pstmt.executeUpdate();
        }
    }

    public void removeMigrationRecord(Connection conn, String patchId) throws Exception {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, patchId);
            pstmt.executeUpdate();
        }
    }
}