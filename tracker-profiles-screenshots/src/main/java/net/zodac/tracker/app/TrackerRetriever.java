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
import java.util.EnumSet;
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
import net.zodac.tracker.framework.config.Configuration;
import net.zodac.tracker.framework.exception.InvalidCsvInputException;
import net.zodac.tracker.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles retrieving and organizing trackers from the CSV input file.
 */
final class TrackerRetriever {

    private static final ApplicationConfiguration CONFIG = Configuration.get();
    private static final Logger LOGGER = LogManager.getLogger();

    private TrackerRetriever() {

    }

    /**
     * Reads trackers from the CSV file and organizes them by type.
     *
     * @return map of tracker types to their credentials
     */
    static Map<TrackerType, Pair<TrackerHandler, Set<TrackerCredential>>> getTrackers() {
        LOGGER.trace("Retrieving trackers to execute");
        final Set<TrackerType> trackerExecutionOrder = EnumSet.copyOf(CONFIG.trackerExecutionOrder());
        LOGGER.debug("Tracker execution order: {}", trackerExecutionOrder);

        try {
            final List<TrackerCredential> trackerCredentials = TrackerCsvReader.readTrackerInfo();
            final Map<TrackerType, Pair<TrackerHandler, Set<TrackerCredential>>> trackersByType = new EnumMap<>(TrackerType.class);

            for (final TrackerCredential trackerCredential : trackerCredentials) {
                final Optional<TrackerHandler> trackerHandlerOptional = TrackerHandlerFactory.findMatchingHandler(trackerCredential.name());
                if (trackerHandlerOptional.isEmpty()) {
                    LOGGER.warn("No implementation found for tracker '{}'", trackerCredential.name());
                    continue;
                }

                final TrackerHandler trackerHandler = trackerHandlerOptional.get();
                final TrackerType trackerType = trackerHandler.type();
                if (!trackerExecutionOrder.contains(trackerType)) {
                    LOGGER.debug("Skipping {} ({})", trackerHandler.name(), trackerType);
                    continue;
                }

                if (!CONFIG.enableAdultContent() && trackerHandler.adult()) {
                    LOGGER.debug("Skipping adult tracker {}", trackerHandler.name());
                    continue;
                }

                final Pair<TrackerHandler, Set<TrackerCredential>> existingPair = trackersByType.get(trackerType);
                if (existingPair == null) {
                    final Set<TrackerCredential> trackerSet = new TreeSet<>();
                    trackerSet.add(trackerCredential);
                    trackersByType.put(trackerType, Pair.of(trackerHandler, trackerSet));
                } else {
                    existingPair.second().add(trackerCredential);
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
     * Prints summary information about all trackers, if {@link Logger#isDebugEnabled()} is {@code true}.
     *
     * @param trackersByType the map of trackers organized by type
     */
    static void printTrackersInfo(final Map<TrackerType, Pair<TrackerHandler, Set<TrackerCredential>>> trackersByType,
                                  final List<TrackerType> trackerExecutionOrder
    ) {
        if (LOGGER.isDebugEnabled()) {
            for (final TrackerType trackerType : trackerExecutionOrder) {
                trackerType.printSummary(trackersByType);
            }
        }
    }
}
