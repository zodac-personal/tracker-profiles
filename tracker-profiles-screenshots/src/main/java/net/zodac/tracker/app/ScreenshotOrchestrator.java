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

import java.io.File;
import java.util.Map;
import java.util.Set;
import net.zodac.tracker.framework.ExitState;
import net.zodac.tracker.framework.TrackerCredential;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import net.zodac.tracker.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main orchestrator class which coordinates taking screenshots of the profile page of each tracker
 * listed in the {@link ApplicationConfiguration#trackerInputFilePath()} file.
 */
public final class ScreenshotOrchestrator {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ApplicationConfiguration CONFIG = Configuration.get();

    private ScreenshotOrchestrator() {

    }

    /**
     * Orchestrates the profile screenshot process by:
     * <ol>
     *   <li>Retrieving trackers from the CSV file</li>
     *   <li>Ensuring the output directory exists</li>
     *   <li>Executing screenshots for each tracker in order</li>
     *   <li>Collecting and reporting results</li>
     * </ol>
     *
     * @return the exit code
     */
    public static ExitState start() {
        final Map<TrackerType, Set<TrackerCredential>> trackersByType = TrackerRetriever.getTrackers();
        final int numberOfTrackers = TrackerRetriever.countAllEnabled(trackersByType, CONFIG);
        if (numberOfTrackers == 0) {
            LOGGER.error("No trackers selected!");
            return ExitState.FAILURE;
        }

        LOGGER.info("Screenshotting {} tracker{}", numberOfTrackers, StringUtils.pluralise(numberOfTrackers));
        ensureOutputDirectoryExists();

        TrackerRetriever.printTrackersInfo(trackersByType, CONFIG);
        final ResultCollector resultCollector = new ResultCollector();

        // Execute in the order specified
        for (final TrackerType trackerType : CONFIG.trackerExecutionOrder()) {
            if (!trackerType.isEnabled(trackersByType, CONFIG)) {
                continue;
            }

            LOGGER.info("");
            LOGGER.info(">>> Executing {} trackers <<<", trackerType.formattedName());
            for (final TrackerCredential trackerCredential : trackersByType.getOrDefault(trackerType, Set.of())) {
                final boolean successfullyTakenScreenshot = ProfileScreenshotExecutor.isSuccessfullyScreenshot(trackerCredential);
                resultCollector.addResult(trackerType, trackerCredential.name(), successfullyTakenScreenshot);
            }
        }

        return resultCollector.generateSummary();
    }

    private static void ensureOutputDirectoryExists() {
        final File outputDirectory = CONFIG.outputDirectory().toFile();
        if (!outputDirectory.exists()) {
            LOGGER.trace("Creating output directory: '{}'", outputDirectory);
            final boolean wasOutputDirectoryCreated = outputDirectory.mkdirs();
            if (!wasOutputDirectoryCreated) {
                LOGGER.trace("Could not create output directory (or already exists): '{}'", outputDirectory);
            }
        }
    }
}