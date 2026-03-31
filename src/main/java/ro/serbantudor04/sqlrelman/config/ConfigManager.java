package ro.serbantudor04.sqlrelman.config;

import ro.serbantudor04.sqlrelman.cli.annotations.ConfigProperty;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".sqlrelman";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";

    private final Properties properties;
    private static ConfigManager instance;

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private ConfigManager() {
        properties = new Properties();
        loadPropertiesFromFile();
    }

    private void loadPropertiesFromFile() {
        try {
            File file = new File(CONFIG_FILE);
            if (file.exists()) {
                try (InputStream input = new FileInputStream(file)) {
                    properties.load(input);
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to load configuration file: " + ex.getMessage());
        }
    }

    /**
     * Injects the loaded properties into the fields of the target object using reflection.
     */
    public void bind(Object configObj) {
        Field[] fields = configObj.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(ConfigProperty.class)) continue;

            ConfigProperty annotation = field.getAnnotation(ConfigProperty.class);
            String value = properties.getProperty(annotation.key(), annotation.defaultValue());

            field.setAccessible(true);
            try {
                field.set(configObj, value);
            } catch (IllegalAccessException e) {
                System.err.println("Could not bind property to field: " + field.getName());
            }
        }
    }

    /**
     * Extracts values from the target object's annotated fields and saves them to the file.
     */
    public void save(Object configObj) {
        Field[] fields = configObj.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(ConfigProperty.class)) continue;

            ConfigProperty annotation = field.getAnnotation(ConfigProperty.class);
            field.setAccessible(true);

            try {
                Object value = field.get(configObj);
                if (value != null) {
                    properties.setProperty(annotation.key(), value.toString());
                }
            } catch (IllegalAccessException e) {
                System.err.println("Could not read property from field: " + field.getName());
            }
        }

        writeToFile();
    }

    private void writeToFile() {
        try {
            Path dirPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
                properties.store(output, "SQLRelMan Dynamic Configuration");
            }
        } catch (IOException ex) {
            System.err.println("Failed to save configuration: " + ex.getMessage());
        }
    }
}