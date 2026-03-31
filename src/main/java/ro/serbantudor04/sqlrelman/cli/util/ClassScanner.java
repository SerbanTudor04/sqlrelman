package ro.serbantudor04.sqlrelman.cli.util;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassScanner {

    public static List<Class<?>> getClassesWithAnnotation(String packageName, Class<? extends java.lang.annotation.Annotation> annotation) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    String decodedPath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
                    findClassesInDirectory(new File(decodedPath), packageName, annotation, classes);
                } else if ("jar".equals(protocol)) {
                    String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                    String decodedPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);
                    try (JarFile jarFile = new JarFile(decodedPath)) {
                        findClassesInJar(jarFile, packageName, annotation, classes);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to scan classes in package: " + packageName);
            e.printStackTrace();
        }
        return classes;
    }

    private static void findClassesInDirectory(File directory, String packageName, Class<? extends java.lang.annotation.Annotation> annotation, List<Class<?>> classes) throws Exception {
        if (!directory.exists()) return;
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + "." + file.getName(), annotation, classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(annotation) && !clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    classes.add(clazz);
                }
            }
        }
    }

    private static void findClassesInJar(JarFile jarFile, String packageName, Class<? extends java.lang.annotation.Annotation> annotation, List<Class<?>> classes) throws Exception {
        String path = packageName.replace('.', '/');
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.startsWith(path) && entryName.endsWith(".class")) {
                String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(annotation) && !clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    classes.add(clazz);
                }
            }
        }
    }
}