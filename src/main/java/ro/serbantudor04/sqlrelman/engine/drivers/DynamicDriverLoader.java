package ro.serbantudor04.sqlrelman.engine.drivers;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads JDBC driver JARs at runtime using a URLClassLoader.
 *
 * Standard Class.forName() won't work for JARs not on the bootstrap classpath,
 * so we wrap the driver in a shim that delegates through the custom class loader.
 * This is the same technique used by Flyway, Liquibase, and similar tools.
 */
public class DynamicDriverLoader {

    // Cache loaded class loaders so we don't re-open the same JAR repeatedly
    private static final Map<String, URLClassLoader> loaderCache = new HashMap<>();

    /**
     * Opens a JDBC connection using the driver JAR from the local cache.
     *
     * @param jarFile    The downloaded driver JAR
     * @param driverClass Fully-qualified driver class name
     * @param jdbcUrl    JDBC connection URL
     * @param user       Database username (may be empty)
     * @param password   Database password (may be empty)
     */
    public static Connection connect(File jarFile, String driverClass,
                                     String jdbcUrl, String user, String password)
            throws Exception {

        URLClassLoader loader = getOrCreateLoader(jarFile);

        // Load the driver class through our custom loader
        Class<?> clazz = Class.forName(driverClass, true, loader);
        Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();

        // Wrap in a shim so DriverManager (which uses system classloader) can see it
        DriverShim shim = new DriverShim(driver);

        // Deregister any previously registered shims for this URL to avoid conflicts
        try {
            DriverManager.deregisterDriver(shim);
        } catch (Exception ignored) {}

        DriverManager.registerDriver(shim);

        Properties props = new Properties();
        if (user != null && !user.isEmpty())     props.setProperty("user", user);
        if (password != null && !password.isEmpty()) props.setProperty("password", password);

        return DriverManager.getConnection(jdbcUrl, props);
    }

    private static URLClassLoader getOrCreateLoader(File jarFile) throws Exception {
        String key = jarFile.getAbsolutePath();
        if (!loaderCache.containsKey(key)) {
            URL jarUrl = jarFile.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{jarUrl},
                    DynamicDriverLoader.class.getClassLoader()
            );
            loaderCache.put(key, loader);
        }
        return loaderCache.get(key);
    }

    /**
     * Shim that bridges a driver loaded by a custom ClassLoader into the
     * system DriverManager. Without this, DriverManager refuses to use
     * drivers from non-system class loaders.
     */
    private static class DriverShim implements Driver {
        private final Driver delegate;

        DriverShim(Driver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return delegate.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return delegate.acceptsURL(url);
        }

        @Override
        public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return delegate.getPropertyInfo(url, info);
        }

        @Override public int getMajorVersion()  { return delegate.getMajorVersion(); }
        @Override public int getMinorVersion()  { return delegate.getMinorVersion(); }
        @Override public boolean jdbcCompliant(){ return delegate.jdbcCompliant(); }
        @Override public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }
}