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

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.zodac.tracker.framework.ExitState;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Collects and reports on the results of screenshot attempts.
 */
final class ResultCollector {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<TrackerType, Collection<String>> successfulTrackers = new EnumMap<>(TrackerType.class);
    private final Map<TrackerType, Collection<String>> unsuccessfulTrackers = new EnumMap<>(TrackerType.class);

    /**
     * Records the result of a screenshot attempt.
     *
     * @param trackerType   the type of tracker
     * @param trackerName   the name of the tracker
     * @param wasSuccessful whether the screenshot was successful
     */
    void addResult(final TrackerType trackerType, final String trackerName, final boolean wasSuccessful) {
        final Map<TrackerType, Collection<String>> targetMap = wasSuccessful ? successfulTrackers : unsuccessfulTrackers;
        targetMap
            .computeIfAbsent(trackerType, _ -> new TreeSet<>())
            .add(trackerName);
    }

    /**
     * Generates a summary of all results and returns the appropriate {@link ExitState}.
     *
     * @param trackerExecutionOrder the execution order of the different {@link TrackerType}s
     * @return the {@link ExitState} based on success/failure counts
     */
    ExitState generateSummary(final List<TrackerType> trackerExecutionOrder) {
        final int totalSuccessful = successfulTrackers.values()
            .stream()
            .mapToInt(Collection::size)
            .sum();

        final int totalUnsuccessful = unsuccessfulTrackers.values()
            .stream()
            .mapToInt(Collection::size)
            .sum();

        if (totalSuccessful == 0 && totalUnsuccessful == 0) {
            LOGGER.error("");
            LOGGER.error("Unexpectedly had no trackers execute");
            return ExitState.FAILURE;
        }

        if (totalSuccessful == 0) {
            LOGGER.error("");
            LOGGER.error("All {} selected tracker{} failed:", totalUnsuccessful, StringUtils.pluralise(totalUnsuccessful));

            unsuccessfulTrackers.forEach((type, trackers) -> {
                LOGGER.error("- {}:", type.formattedName());
                trackers.forEach(name -> LOGGER.error("\t- {}", name));
            });

            return ExitState.FAILURE;
        }

        if (totalUnsuccessful == 0) {
            LOGGER.info("");
            LOGGER.info("All {} selected tracker{} successfully screenshot", totalSuccessful, StringUtils.pluralise(totalSuccessful));

            successfulTrackers.forEach((type, trackers) -> LOGGER.debug("- {} ({}): {}", type.formattedName(), trackers.size(), trackers));

            return ExitState.SUCCESS;
        }

        LOGGER.warn("");
        LOGGER.warn("Failures for following tracker{}:", StringUtils.pluralise(totalUnsuccessful));

        for (final TrackerType trackerType : trackerExecutionOrder) {
            final Collection<String> trackers = unsuccessfulTrackers.get(trackerType);
            if (trackers == null || trackers.isEmpty()) {
                continue;
            }

            LOGGER.warn("- {}:", trackerType.formattedName());
            trackers.forEach(name -> LOGGER.warn("\t- {}", name));
        }

        return ExitState.PARTIAL_FAILURE;
    }
}
