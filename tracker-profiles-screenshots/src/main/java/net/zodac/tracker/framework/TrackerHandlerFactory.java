/*
 * BSD Zero Clause License
 *
 * Copyright (c) 2024-2026 zodac.net
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package net.zodac.tracker.framework;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.annotation.TrackerHandlers;
import net.zodac.tracker.handler.AbstractTrackerHandler;
import org.jspecify.annotations.Nullable;

/**
 * Factory class used to create an instance of a {@link AbstractTrackerHandler}.
 */
public final class TrackerHandlerFactory {

    private static final Map<String, Map.Entry<Class<?>, TrackerHandler>> TRACKER_HANDLES_BY_NAME = buildHandlerMap();

    private TrackerHandlerFactory() {

    }

    /**
     * Checks if an implementation of {@link AbstractTrackerHandler} exists that matches the wanted {@code trackerName}.
     *
     * @param trackerName the name of the tracker for which we want a {@link AbstractTrackerHandler}
     * @return {@link Optional} {@link TrackerHandler} for a matching {@code trackerName}
     */
    public static Optional<TrackerHandler> findMatchingHandler(final String trackerName) {
        return Optional.ofNullable(TRACKER_HANDLES_BY_NAME.get(trackerName.toLowerCase(Locale.ROOT)))
            .map(Map.Entry::getValue);
    }

    /**
     * Finds an implementation of {@link AbstractTrackerHandler} that matches the wanted {@code trackerName}, and returns an instance of it.
     * Implementations of {@link AbstractTrackerHandler} should be annotated by at least one {@link TrackerHandler}, which contains a
     * {@link TrackerHandler#name()}, which should match the input (the match is case-insensitive).
     *
     * @param trackerName the name of the tracker for which we want a {@link AbstractTrackerHandler}
     * @return an instance of the matching {@link AbstractTrackerHandler}
     * @throws IllegalStateException  thrown if an error occurred when instantiating the {@link AbstractTrackerHandler}
     * @throws NoSuchElementException thrown if no valid {@link AbstractTrackerHandler} implementation could be found
     */
    public static AbstractTrackerHandler getHandler(final String trackerName) {
        final var entry = TRACKER_HANDLES_BY_NAME.get(trackerName.toLowerCase(Locale.ROOT));
        if (entry == null) {
            throw new NoSuchElementException(
                "Unable to find %s with name '%s'".formatted(TrackerHandler.class.getSimpleName(), trackerName));
        }

        final TrackerDefinition trackerDefinition = TrackerDefinition.fromAnnotation(entry.getValue());
        return makeNewInstance(entry.getKey(), trackerDefinition);
    }

    private static Map<String, Map.Entry<Class<?>, TrackerHandler>> buildHandlerMap() {
        final Map<String, Map.Entry<Class<?>, TrackerHandler>> map = new HashMap<>();
        for (final Class<?> clazz : findAllClassesUsingClassLoader(AbstractTrackerHandler.class.getPackageName())) {
            for (final TrackerHandler annotation : clazz.getAnnotationsByType(TrackerHandler.class)) {
                map.put(annotation.name().toLowerCase(Locale.ROOT), Map.entry(clazz, annotation));
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static AbstractTrackerHandler makeNewInstance(final Class<?> trackerHandler, final TrackerDefinition trackerDefinition) {
        try {
            final AbstractTrackerHandler abstractTrackerHandler = (AbstractTrackerHandler) trackerHandler.getDeclaredConstructor().newInstance();
            abstractTrackerHandler.configure(trackerDefinition);
            return abstractTrackerHandler;
        } catch (final IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Error instantiating an instance of '%s'".formatted(trackerHandler), e);
        }
    }

    private static Set<Class<?>> findAllClassesUsingClassLoader(final String packageName) {
        final String packagePath = packageName.replace('.', '/');

        try {
            final URL resource = Thread.currentThread().getContextClassLoader().getResource(packagePath);
            if (resource == null) {
                throw new IllegalStateException("Unable to retrieve classes from package '%s'".formatted(packageName));
            }

            // If not running from a JAR, assume classes are available on the filesystem
            return "jar".equals(resource.getProtocol()) ? getFromJar(resource, packagePath) : getFromFile(resource, packageName);
        } catch (final IOException | URISyntaxException e) {
            throw new IllegalStateException("Unable to retrieve classes from package '%s'".formatted(packageName), e);
        }
    }

    private static Set<Class<?>> getFromFile(final URL resource, final String packageName) throws URISyntaxException {
        final File[] directoryFiles = new File(resource.toURI()).listFiles();
        if (directoryFiles == null || directoryFiles.length == 0) {
            throw new IllegalStateException("Unable to retrieve classes from resource '%s'".formatted(resource.toURI()));
        }

        return Arrays.stream(directoryFiles)
            .map(File::getName)
            .filter(fileName -> fileName.endsWith(".class"))
            .map(fileName -> getClass(fileName, packageName))
            .filter(aClass -> aClass.isAnnotationPresent(TrackerHandler.class) || aClass.isAnnotationPresent(TrackerHandlers.class))
            .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Class::toString))));
    }

    private static Set<Class<?>> getFromJar(final URL resource, final String packagePath) throws IOException {
        final JarURLConnection connection = (JarURLConnection) resource.openConnection();
        connection.setUseCaches(false);

        try (final JarFile jarFile = connection.getJarFile()) {
            return jarFile.stream()
                .map(ZipEntry::getName)
                .filter(jarEntryName -> jarEntryName.startsWith(packagePath) && jarEntryName.endsWith(".class"))
                .map(jarEntryName -> getClass(jarEntryName.replace('/', '.'), null))
                .filter(aClass -> aClass.isAnnotationPresent(TrackerHandler.class) || aClass.isAnnotationPresent(TrackerHandlers.class))
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Class::toString))));
        }
    }

    private static Class<?> getClass(final String className, final @Nullable String packageName) {
        try {
            final String prefix = packageName == null ? "" : (packageName + ".");
            return Class.forName(prefix + className.substring(0, className.lastIndexOf('.')));
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Unable to retrieve class '%s' from package '%s'".formatted(className, packageName), e);
        }
    }
}
