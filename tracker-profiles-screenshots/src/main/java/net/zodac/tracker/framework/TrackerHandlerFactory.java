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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
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

    private static final Set<Class<?>> TRACKER_HANDLER_CLASSES = findAllClassesUsingClassLoader(AbstractTrackerHandler.class.getPackageName());

    private TrackerHandlerFactory() {

    }

    /**
     * Checks if an implementation of {@link AbstractTrackerHandler} exists that matches the wanted {@code trackerName}.
     *
     * @param trackerName the name of the tracker for which we want a {@link AbstractTrackerHandler}
     * @return {@link Optional} {@link TrackerHandler} for a matching {@code trackerName}
     */
    public static Optional<TrackerHandler> findMatchingHandler(final String trackerName) {
        return TRACKER_HANDLER_CLASSES.stream()
            .flatMap(handler -> Arrays.stream(handler.getAnnotationsByType(TrackerHandler.class)))
            .filter(annotation -> annotation.name().equalsIgnoreCase(trackerName))
            .findAny();
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
        final var matchingTrackerHandlerOptional = TRACKER_HANDLER_CLASSES.stream()
            .flatMap(handler -> Arrays.stream(handler.getAnnotationsByType(TrackerHandler.class))
                .map(annotation -> Map.entry(handler, annotation))
            )
            .filter(entry -> entry.getValue().name().equalsIgnoreCase(trackerName))
            .findAny();

        if (matchingTrackerHandlerOptional.isEmpty()) {
            final String errorMessage = String.format("Unable to find %s with name '%s'", TrackerHandler.class.getSimpleName(), trackerName);
            throw new NoSuchElementException(errorMessage);
        }

        final var matchingTrackerHandler = matchingTrackerHandlerOptional.get();
        final Class<?> trackerHandler = matchingTrackerHandler.getKey();

        final TrackerHandler annotation = matchingTrackerHandler.getValue();
        final TrackerDefinition trackerDefinition = TrackerDefinition.fromAnnotation(annotation);
        return makeNewInstance(trackerHandler, trackerDefinition);
    }

    private static AbstractTrackerHandler makeNewInstance(final Class<?> trackerHandler, final TrackerDefinition trackerDefinition) {
        try {
            final AbstractTrackerHandler abstractTrackerHandler = (AbstractTrackerHandler) trackerHandler.getDeclaredConstructor().newInstance();
            abstractTrackerHandler.configure(trackerDefinition);
            return abstractTrackerHandler;
        } catch (final IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException(String.format("Error instantiating an instance of '%s'", trackerHandler), e);
        }
    }

    private static Set<Class<?>> findAllClassesUsingClassLoader(final String packageName) {
        final String packagePath = packageName.replace('.', '/');

        try {
            final URL resource = Thread.currentThread().getContextClassLoader().getResource(packagePath);
            if (resource == null) {
                throw new IllegalStateException(String.format("Unable to retrieve classes from package '%s'", packageName));
            }

            // If not running from a JAR, assume classes are available on the filesystem
            return "jar".equals(resource.getProtocol()) ? getFromJar(resource, packagePath) : getFromFile(resource, packageName);
        } catch (final IOException | URISyntaxException e) {
            throw new IllegalStateException(String.format("Unable to retrieve classes from package '%s'", packageName), e);
        }
    }

    private static Set<Class<?>> getFromFile(final URL resource, final String packageName) throws URISyntaxException {
        final File[] directoryFiles = new File(resource.toURI()).listFiles();
        if (directoryFiles == null || directoryFiles.length == 0) {
            throw new IllegalStateException(String.format("Unable to retrieve classes from resource '%s'", resource.toURI()));
        }

        return Arrays.stream(directoryFiles)
            .map(File::getName)
            .filter(fileName -> fileName.endsWith(".class"))
            .map(fileName -> getClass(fileName, packageName))
            .filter(aClass -> aClass.isAnnotationPresent(TrackerHandler.class) || aClass.isAnnotationPresent(TrackerHandlers.class))
            .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Class::toString))));
    }

    private static Set<Class<?>> getFromJar(final URL resource, final String packagePath) throws IOException {
        final String jarFilePath = resource.getFile().substring(5, resource.getFile().indexOf('!'));

        try (final JarFile jarFile = new JarFile(URLDecoder.decode(jarFilePath, StandardCharsets.UTF_8))) {
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
            throw new IllegalStateException(String.format("Unable to retrieve class '%s' from package '%s'", className, packageName), e);
        }
    }
}
