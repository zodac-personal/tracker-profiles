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

package net.zodac.tracker.app;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.zodac.tracker.framework.TrackerCredential;
import net.zodac.tracker.framework.TrackerCsvReader;
import net.zodac.tracker.framework.TrackerHandlerFactory;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.exception.InvalidCsvInputException;
import net.zodac.tracker.handler.AbstractTrackerHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles retrieving and organizing trackers from the CSV input file.
 */
final class TrackerRetriever {

    private static final Logger LOGGER = LogManager.getLogger();

    private TrackerRetriever() {

    }

    /**
     * Reads trackers from the CSV file and organizes them by type.
     *
     * @return map of tracker types to their credentials
     */
    static Map<TrackerType, Set<TrackerCredential>> getTrackers() {
        try {
            final List<TrackerCredential> trackerCredentials = TrackerCsvReader.readTrackerInfo();
            final Map<TrackerType, Set<TrackerCredential>> trackersByType = new EnumMap<>(TrackerType.class);

            for (final TrackerCredential trackerCredential : trackerCredentials) {
                final Optional<TrackerHandler> trackerHandler = TrackerHandlerFactory.findMatchingHandler(trackerCredential.name());

                if (trackerHandler.isPresent()) {
                    final TrackerType trackerType = trackerHandler.get().type();
                    final Set<TrackerCredential> existingTrackerDefinitionsOfType = trackersByType.getOrDefault(trackerType, new TreeSet<>());
                    existingTrackerDefinitionsOfType.add(trackerCredential);
                    trackersByType.put(trackerType, existingTrackerDefinitionsOfType);
                } else {
                    LOGGER.warn("No {} implemented for tracker '{}'", AbstractTrackerHandler.class.getSimpleName(), trackerCredential.name());
                }
            }

            return trackersByType;
        } catch (final InvalidCsvInputException e) {
            LOGGER.warn("Error with CSV input file content", e);
            return Map.of();
        } catch (final IOException e) {
            LOGGER.warn("Unable to read CSV input file", e);
            return Map.of();
        }
    }

    /**
     * Counts all enabled trackers across all {@link TrackerType}s.
     *
     * @param trackersByType the map of trackers organized by type
     * @param config         the application configuration
     * @return total number of enabled trackers
     */
    static int countAllEnabled(final Map<TrackerType, Set<TrackerCredential>> trackersByType, final ApplicationConfiguration config) {
        return TrackerType.ALL_VALUES
            .stream()
            .filter(trackerType -> trackerType.isEnabled(trackersByType, config))
            .mapToInt(trackerType -> trackersByType.getOrDefault(trackerType, Set.of()).size())
            .sum();
    }

    /**
     * Prints summary information about all trackers, if {@link Logger#isDebugEnabled()} is {@code true}.
     *
     * @param trackersByType the map of trackers organized by type
     * @param config         the application configuration
     */
    static void printTrackersInfo(final Map<TrackerType, Set<TrackerCredential>> trackersByType, final ApplicationConfiguration config) {
        if (LOGGER.isDebugEnabled()) {
            for (final TrackerType trackerType : config.trackerExecutionOrder()) {
                trackerType.printSummary(trackersByType, config);
            }
        }
    }
}