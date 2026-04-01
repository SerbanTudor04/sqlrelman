package ro.serbantudor04.sqlrelman.engine.drivers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Downloads JDBC driver JARs from Maven Central and caches them locally
 * under ~/.sqlrelman/drivers/.
 */
public class DriverDownloader {

    private static final String DRIVERS_DIR =
            System.getProperty("user.home") + File.separator + ".sqlrelman" + File.separator + "drivers";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Returns the cached JAR file for the given driver, downloading it first if needed.
     *
     * @param info  Driver metadata from DriverRegistry
     * @param force Re-download even if already cached
     * @return The local JAR file, or null if download failed
     */
    public static File ensureDriver(DriverRegistry.DriverInfo info, boolean force) {
        File driversDir = new File(DRIVERS_DIR);
        if (!driversDir.exists()) {
            driversDir.mkdirs();
        }

        File jarFile = new File(driversDir, info.jarFileName());

        if (jarFile.exists() && !force) {
            System.out.println("Driver already cached: " + jarFile.getName());
            return jarFile;
        }

        String url = info.mavenJarUrl();
        System.out.println("Downloading driver from Maven Central...");
        System.out.println("  -> " + url);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            int status = response.statusCode();
            if (status != 200) {
                System.err.println("Download failed: HTTP " + status + " for " + url);
                return null;
            }

            long totalBytes = response.headers()
                    .firstValueAsLong("content-length")
                    .orElse(-1L);

            try (InputStream in = response.body();
                 FileOutputStream out = new FileOutputStream(jarFile)) {

                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int read;

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    downloaded += read;
                    printProgress(downloaded, totalBytes);
                }
                System.out.println(); // newline after progress
            }

            System.out.println("Driver saved: " + jarFile.getAbsolutePath());
            return jarFile;

        } catch (IOException | InterruptedException e) {
            System.err.println("Download error: " + e.getMessage());
            if (jarFile.exists()) jarFile.delete(); // delete partial file
            return null;
        }
    }

    /** Returns the cached driver JAR if it exists, null otherwise. */
    public static File getCachedDriver(DriverRegistry.DriverInfo info) {
        File jar = new File(DRIVERS_DIR, info.jarFileName());
        return jar.exists() ? jar : null;
    }

    private static void printProgress(long downloaded, long total) {
        if (total <= 0) {
            System.out.printf("\r  Downloaded: %.1f KB", downloaded / 1024.0);
        } else {
            int pct = (int) (downloaded * 100 / total);
            int filled = pct / 5;
            String bar = "=".repeat(filled) + " ".repeat(20 - filled);
            System.out.printf("\r  [%s] %d%%  (%.1f / %.1f KB)", bar, pct,
                    downloaded / 1024.0, total / 1024.0);
        }
    }
}