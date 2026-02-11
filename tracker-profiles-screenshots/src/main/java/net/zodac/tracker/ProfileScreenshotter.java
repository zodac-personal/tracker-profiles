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

package net.zodac.tracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.zodac.tracker.framework.ExitState;
import net.zodac.tracker.framework.TrackerCredential;
import net.zodac.tracker.framework.TrackerCsvReader;
import net.zodac.tracker.framework.TrackerHandlerFactory;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import net.zodac.tracker.framework.exception.BrowserClosedException;
import net.zodac.tracker.framework.exception.CancelledInputException;
import net.zodac.tracker.framework.exception.DriverAttachException;
import net.zodac.tracker.framework.exception.InvalidCsvInputException;
import net.zodac.tracker.framework.exception.NoUserInputException;
import net.zodac.tracker.framework.exception.TranslationException;
import net.zodac.tracker.handler.AbstractTrackerHandler;
import net.zodac.tracker.util.ScreenshotTaker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.UnreachableBrowserException;

/**
 * Main driver class, which takes a screenshot of the profile page of each tracker listed in the
 * {@link ApplicationConfiguration#trackerInputFilePath()} file.
 */
@SuppressWarnings("OverlyLongMethod")
public final class ProfileScreenshotter {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ApplicationConfiguration CONFIG = Configuration.get();

    private ProfileScreenshotter() {

    }

    /**
     * Parses the {@link ApplicationConfiguration#trackerInputFilePath()} file using {@link TrackerCsvReader}, then iterates through each
     * {@link TrackerCredential}. For each tracker a {@link AbstractTrackerHandler} is retrieved and used to navigate to the tracker's profile page
     * (after logging in and any other required actions). At this point, any sensitive information is redacted, and then a screenshot is taken by
     * {@link ScreenshotTaker}, then saved in the {@link ApplicationConfiguration#outputDirectory()}.
     *
     * @return the exit code
     * @see ScreenshotTaker
     */
    public static ExitState executeProfileScreenshotter() {
        final Map<TrackerType, Set<TrackerCredential>> trackersByType = getTrackers();
        final int numberOfTrackers = countAllEnabled(trackersByType);
        final String trackersPlural = numberOfTrackers == 1 ? "" : "s";

        if (numberOfTrackers == 0) {
            LOGGER.error("No trackers selected!");
            return ExitState.FAILURE;
        }
        LOGGER.info("Screenshotting {} tracker{}", numberOfTrackers, trackersPlural);

        final File outputDirectory = CONFIG.outputDirectory().toFile();
        if (!outputDirectory.exists()) {
            LOGGER.trace("Creating output directory: '{}'", outputDirectory);
            final boolean wasOutputDirectoryCreated = outputDirectory.mkdirs();
            if (!wasOutputDirectoryCreated) {
                LOGGER.trace("Could not create output directory (or already exists): '{}'", outputDirectory);
            }
        }

        printTrackersInfo(trackersByType);
        final Collection<String> successfulTrackers = new TreeSet<>();
        final Collection<String> unsuccessfulTrackers = new TreeSet<>();

        // Execute in the order specified
        for (final TrackerType trackerType : CONFIG.trackerExecutionOrder()) {
            if (!trackerType.isEnabled(trackersByType, CONFIG)) {
                continue;
            }

            LOGGER.info("");
            LOGGER.info(">>> Executing {} trackers <<<", trackerType.formattedName());
            for (final TrackerCredential trackerCredential : trackersByType.getOrDefault(trackerType, Set.of())) {
                final boolean successfullyTakenScreenshot = isAbleToTakeScreenshot(trackerCredential);
                if (successfullyTakenScreenshot) {
                    successfulTrackers.add(trackerCredential.name());
                } else {
                    unsuccessfulTrackers.add(trackerCredential.name());
                }
            }
        }

        return returnResultSummary(successfulTrackers, unsuccessfulTrackers);
    }

    private static ExitState returnResultSummary(final Collection<String> successfulTrackers, final Collection<String> unsuccessfulTrackers) {
        if (successfulTrackers.isEmpty()) {
            final String trackersPlural = unsuccessfulTrackers.size() == 1 ? "" : "s";
            LOGGER.error("");
            LOGGER.error("All {} selected tracker{} failed:", unsuccessfulTrackers.size(), trackersPlural);
            for (final String unsuccessfulTracker : unsuccessfulTrackers) {
                LOGGER.error("\t- {}", unsuccessfulTracker);
            }
            return ExitState.FAILURE;
        }

        if (unsuccessfulTrackers.isEmpty()) {
            final String trackersPlural = successfulTrackers.size() == 1 ? "" : "s";
            LOGGER.info("");
            LOGGER.info("{} tracker{} successfully screenshot", successfulTrackers.size(), trackersPlural);
            return ExitState.SUCCESS;
        } else {
            final String trackersPlural = unsuccessfulTrackers.size() == 1 ? "" : "s";
            LOGGER.warn("");
            // TODO: Print these by type
            LOGGER.warn("Failures for following tracker{}:", trackersPlural);
            for (final String unsuccessfulTracker : unsuccessfulTrackers) {
                LOGGER.warn("\t- {}", unsuccessfulTracker);
            }
            return ExitState.PARTIAL_FAILURE;
        }
    }

    private static void printTrackersInfo(final Map<TrackerType, Set<TrackerCredential>> trackersByType) {
        if (LOGGER.isDebugEnabled()) {
            for (final TrackerType trackerType : CONFIG.trackerExecutionOrder()) {
                trackerType.printSummary(trackersByType, CONFIG);
            }
        }
    }

    private static int countAllEnabled(final Map<TrackerType, Set<TrackerCredential>> trackersByType) {
        return TrackerType.ALL_VALUES
            .stream()
            .filter(trackerType -> trackerType.isEnabled(trackersByType, CONFIG))
            .mapToInt(trackerType -> trackersByType.getOrDefault(trackerType, Set.of()).size())
            .sum();
    }

    private static Map<TrackerType, Set<TrackerCredential>> getTrackers() {
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

    private static boolean isAbleToTakeScreenshot(final TrackerCredential trackerCredential) {
        LOGGER.info("");
        LOGGER.info("[{}]", trackerCredential.name());

        // TODO: Add a retry option
        // TODO: On failure, take a screenshot and add to a subdirectory
        try (final AbstractTrackerHandler trackerHandler = TrackerHandlerFactory.getHandler(trackerCredential.name())) {
            screenshotProfile(trackerHandler, trackerCredential);
            return true;
        } catch (final CancelledInputException e) {
            LOGGER.debug("\t- User cancelled manual input for tracker '{}'", trackerCredential.name(), e);
            LOGGER.warn("\t- User cancelled manual input for tracker '{}'", trackerCredential.name());
            return false;
        } catch (final DriverAttachException e) {
            LOGGER.debug("\t- Unable to attach to Python Selenium web browser for tracker '{}'", trackerCredential.name(), e);
            LOGGER.warn("\t- Unable to attach to Python Selenium web browser for tracker '{}'", trackerCredential.name());
            return false;
        } catch (final FileNotFoundException e) {
            LOGGER.debug("\t- Unable to find expected file for tracker '{}'", trackerCredential.name());
            LOGGER.warn("\t- Unable to find expected file for tracker '{}': {}", trackerCredential.name(), e.getMessage());
            return false;
        } catch (final NoSuchElementException e) {
            LOGGER.debug("\t- No implementation for tracker '{}'", trackerCredential.name(), e);
            LOGGER.warn("\t- No implementation for tracker '{}'", trackerCredential.name());
            return false;
        } catch (final NoUserInputException e) {
            LOGGER.debug("\t- User provided no manual input for tracker '{}'", trackerCredential.name(), e);
            LOGGER.warn("\t- User provided no manual input for tracker '{}'", trackerCredential.name());
            return false;
        } catch (final TimeoutException e) {
            LOGGER.debug("\t- Timed out waiting to find required element for tracker '{}'", trackerCredential.name(), e);
            if (e.getMessage() == null) {
                LOGGER.warn("\t- Timed out waiting to find required element for tracker '{}'", trackerCredential.name());
            } else {
                final String errorMessage = e.getMessage().split("\n")[0];
                LOGGER.warn("\t- Timed out waiting to find required element for tracker '{}': {}", trackerCredential.name(), errorMessage);
            }
            return false;
        } catch (final TranslationException e) {
            LOGGER.debug("\t- Unable to translate tracker '{}' to English", trackerCredential.name(), e);
            LOGGER.warn("\t- Unable to translate tracker '{}' to English: {}", trackerCredential.name(), e.getMessage());
            return false;
        } catch (final NoSuchSessionException | UnreachableBrowserException e) {
            LOGGER.debug("Browser unavailable, most likely user-cancelled", e);
            throw new BrowserClosedException(e);
        } catch (final Exception e) {
            LOGGER.debug("\t- Unexpected error taking screenshot of '{}'", trackerCredential.name(), e);

            if (e.getMessage() == null) {
                LOGGER.warn("\t- Unexpected error taking screenshot of '{}'", trackerCredential.name());
            } else {
                final String errorMessage = e.getMessage().split("\n")[0];
                LOGGER.warn("\t- Unexpected error taking screenshot of '{}': {}", trackerCredential.name(), errorMessage);
            }

            return false;
        }
    }

    private static void screenshotProfile(final AbstractTrackerHandler trackerHandler, final TrackerCredential trackerCredential) throws IOException {
        if (doesScreenshotExistAndSkipSelected(trackerCredential.name())) {
            return;
        }

        LOGGER.info("\t- Opening tracker");
        trackerHandler.openTracker();
        trackerHandler.navigateToLoginPage(trackerCredential.name());

        LOGGER.info("\t- Logging in as '{}'", trackerCredential.username());
        trackerHandler.login(trackerCredential.username(), trackerCredential.password(), trackerCredential.name());

        if (trackerHandler.canBannerBeCleared()) {
            LOGGER.info("\t- Banner has been cleared");
        }

        LOGGER.info("\t- Opening user profile page");
        trackerHandler.openProfilePage();

        // TODO: Take both redacted and non-redacted screenshots?
        if (trackerHandler.hasElementsNeedingRedaction()) {
            LOGGER.info("\t- Redacting elements with sensitive information");
            final int numberOfRedactedElements = trackerHandler.redactElements();
            if (numberOfRedactedElements != 0) {
                final String redactedElementsPlural = numberOfRedactedElements == 1 ? "" : "s";
                LOGGER.info("\t\t- Redacted the text of {} element{}", numberOfRedactedElements, redactedElementsPlural);
            }
        }

        if (trackerHandler.hasFixedHeader()) {
            LOGGER.info("\t- Header has been updated to not be fixed");
        }

        if (CONFIG.enableTranslationToEnglish() && trackerHandler.isNotEnglish(trackerCredential.username())) {
            LOGGER.info("\t- Profile page has been translated to English");
        }

        final File screenshot = ScreenshotTaker.takeScreenshot(trackerHandler.driver(), trackerCredential.name());
        LOGGER.info("\t- Screenshot saved at: [{}]", screenshot.getAbsolutePath());

        trackerHandler.logout();
        LOGGER.info("\t- Logged out");
    }

    private static boolean doesScreenshotExistAndSkipSelected(final String trackerName) {
        if (!ScreenshotTaker.doesScreenshotAlreadyExist(trackerName)) {
            return false;
        }

        return switch (CONFIG.existingScreenshotAction()) {
            case OVERWRITE -> {
                LOGGER.debug("\t- Screenshot already exists for tracker '{}', overwriting with new screenshot", trackerName);
                yield false;
            }
            case SKIP -> {
                LOGGER.warn("\t- Screenshot already exists for tracker '{}', skipping", trackerName);
                yield true;
            }
        };
    }
}
