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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.zodac.tracker.framework.ExitState;
import net.zodac.tracker.framework.TrackerCredential;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import net.zodac.tracker.framework.driver.DriverPool;
import net.zodac.tracker.framework.progress.ProgressBarManager;
import net.zodac.tracker.framework.progress.ProgressBarPrintStream;
import net.zodac.tracker.framework.progress.TrackerStep;
import net.zodac.tracker.util.StringUtils;
import net.zodac.tracker.util.TimingUtils;
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
     * @return the {@link ExitState} of the execution
     */
    public static ExitState start(final Map<TrackerType, Set<TrackerCredential>> trackersByType) {
        final int numberOfTrackers = countNumberOfTrackers(trackersByType);
        if (numberOfTrackers == 0) {
            LOGGER.error("No trackers selected!");
            return ExitState.FAILURE;
        }

        ensureOutputDirectoryExists();
        final ResultCollector resultCollector = ResultCollector.start();
        final ProgressBarManager progressBarManager = ProgressBarManager.create();
        try (final ProgressBarPrintStream progressBarPrintStream = new ProgressBarPrintStream(progressBarManager)) {
            System.setOut(progressBarPrintStream);  // Override stdout with the progress bar output
            LOGGER.info("Screenshotting {} tracker{}", numberOfTrackers, StringUtils.pluralise(numberOfTrackers));

            TrackerRetriever.printTrackersInfo(trackersByType, CONFIG.trackerExecutionOrder());
            progressBarManager.start(numberOfTrackers, numberOfTrackers * TrackerStep.NUMBER_OF_STEPS);

            // Get the max length so we don't resize the log entry during execution
            final int maxTrackerNameLength = maxTrackerNameLength(trackersByType);

            // Execute in the order specified
            for (final TrackerType trackerType : CONFIG.trackerExecutionOrder()) {
                screenshotTrackerByType(trackerType, trackersByType, progressBarManager, maxTrackerNameLength, resultCollector);
            }
        } finally {
            DriverPool.shutdown();
        }

        return resultCollector.generateSummary(CONFIG.trackerExecutionOrder());
    }

    private static int countNumberOfTrackers(final Map<TrackerType, Set<TrackerCredential>> trackersByType) {
        return trackersByType.values()
            .stream()
            .mapToInt(Set::size)
            .sum();
    }

    private static void screenshotTrackerByType(final TrackerType trackerType, final Map<TrackerType, Set<TrackerCredential>> trackersByType,
                                                final ProgressBarManager progressBarManager, final int maxTrackerNameLength,
                                                final ResultCollector resultCollector) {
        if (!trackersByType.containsKey(trackerType)) {
            LOGGER.trace("No trackers of type {}", trackerType);
            return;
        }

        LOGGER.info("");
        LOGGER.info(">>> Executing {} trackers <<<", trackerType.formattedName());
        LOGGER.info("");

        // TODO: Min of parallelThreads and tracker count
        DriverPool.initialise(trackerType, CONFIG.numberOfParallelThreads());

        // TODO: Skip parallelism if UI enabled? Maybe add another option to override
        if (trackerType == TrackerType.HEADLESS) {
            final List<Callable<Void>> tasks = new ArrayList<>();
            for (final TrackerCredential tracker : trackersByType.get(trackerType)) {
                tasks.add(() -> {
                    final long startNanos = System.nanoTime();
                    final boolean success = ProfileScreenshotExecutor.takeScreenshot(tracker, progressBarManager, maxTrackerNameLength);
                    resultCollector.addResult(trackerType, tracker.name(), success);
                    printTrackerExecutionTime(tracker.name(), startNanos);
                    progressBarManager.tickTracker(tracker.name());
                    return null;
                });
            }
            try (final ExecutorService executor = Executors.newFixedThreadPool(CONFIG.numberOfParallelThreads())) {
                executor.invokeAll(tasks);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Parallel execution interrupted for {} trackers", trackerType.formattedName());
            }
        } else {
            for (final TrackerCredential tracker : trackersByType.get(trackerType)) {
                final long startNanos = System.nanoTime();
                final boolean success = ProfileScreenshotExecutor.takeScreenshot(tracker, progressBarManager, maxTrackerNameLength);
                resultCollector.addResult(trackerType, tracker.name(), success);
                printTrackerExecutionTime(tracker.name(), startNanos);
                progressBarManager.tickTracker(tracker.name());
            }
        }
    }

    private static int maxTrackerNameLength(final Map<TrackerType, Set<TrackerCredential>> trackersByType) {
        return trackersByType.values().stream()
            .flatMap(Set::stream)
            .mapToInt(credential -> credential.name().length())
            .max()
            .orElse(0);
    }

    private static void printTrackerExecutionTime(final String trackerName, final long startNanos) {
        final long elapsedNanos = System.nanoTime() - startNanos;
        LOGGER.debug("\t- Execution time for {}: {}", trackerName, TimingUtils.toNaturalTime(elapsedNanos));
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
