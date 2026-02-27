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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import net.zodac.tracker.framework.TrackerCredential;
import net.zodac.tracker.framework.TrackerHandlerFactory;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import net.zodac.tracker.framework.config.ExistingScreenshotAction;
import net.zodac.tracker.framework.exception.BrowserClosedException;
import net.zodac.tracker.framework.exception.CancelledInputException;
import net.zodac.tracker.framework.exception.DriverAttachException;
import net.zodac.tracker.framework.exception.NoUserInputException;
import net.zodac.tracker.handler.AbstractTrackerHandler;
import net.zodac.tracker.util.Pair;
import net.zodac.tracker.util.ScreenshotTaker;
import net.zodac.tracker.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.UnreachableBrowserException;

/**
 * Handles the execution of taking a screenshot for a single tracker.
 */
final class ProfileScreenshotExecutor {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ApplicationConfiguration CONFIG = Configuration.get();

    private ProfileScreenshotExecutor() {

    }

    /**
     * Attempts to take a screenshot for the given tracker credential.
     *
     * @param trackerCredential the tracker to screenshot
     * @return true if screenshot was successful, false otherwise
     */
    static boolean isSuccessfullyScreenshot(final TrackerCredential trackerCredential) {
        LOGGER.info("");
        LOGGER.info("[{}]", trackerCredential.name());

        // TODO: Add a retry option, then return number of attempts needed
        AbstractTrackerHandler trackerHandler = null;
        try { // NOPMD: UseTryWithResources - need access to the trackerHandler to take a screenshot on error
            // TODO: Time each tracker
            trackerHandler = TrackerHandlerFactory.getHandler(trackerCredential.name());
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
            screenshotOnError(trackerHandler, trackerCredential.name());
            LOGGER.debug("\t- Timed out waiting to find required element for tracker '{}'", trackerCredential.name(), e);

            final String errorMessage = e.getMessage() == null ? "" : e.getMessage();
            if (errorMessage.isEmpty()) {
                LOGGER.warn("\t- Timed out waiting to find required element for tracker '{}'", trackerCredential.name());
            } else {
                final String cleanedErrorMessage = errorMessage.split("\n")[0];
                LOGGER.warn("\t- Timed out waiting to find required element for tracker '{}': {}", trackerCredential.name(), cleanedErrorMessage);
            }
            return false;
        } catch (final NoSuchSessionException | NoSuchWindowException | UnreachableBrowserException e) {
            LOGGER.debug("Browser unavailable, most likely user-cancelled", e);
            throw new BrowserClosedException(e);
        } catch (final Exception e) {
            screenshotOnError(trackerHandler, trackerCredential.name());
            LOGGER.debug("\t- Unexpected error taking screenshot of '{}'", trackerCredential.name(), e);

            final String errorMessage = e.getMessage() == null ? "" : e.getMessage();
            if (errorMessage.isEmpty()) {
                LOGGER.warn("\t- Unexpected error taking screenshot of '{}'", trackerCredential.name());
            } else {
                final String cleanedErrorMessage = errorMessage.split("\n")[0];
                LOGGER.warn("\t- Unexpected error taking screenshot of '{}': {}", trackerCredential.name(), cleanedErrorMessage);
            }

            return false;
        } finally {
            if (trackerHandler != null) {
                trackerHandler.close();
            }
        }
    }

    private static void screenshotOnError(final @Nullable AbstractTrackerHandler trackerHandler, final String trackerName) {
        if (!CONFIG.takeScreenshotOnError()) {
            LOGGER.trace("Error occurred, but screenshots for errors are not enabled");
            return;
        }

        if (trackerHandler == null) {
            LOGGER.warn("\t- Unable to take failure screenshot, trackerHandler is unexpectedly null");
            return;
        }

        try {
            LOGGER.trace("Taking failure screenshot for '{}'", trackerName);
            final Path failureOutputDirectory = CONFIG.outputDirectory().resolve("errors");
            ensureDirectoryExists(failureOutputDirectory);
            final File screenshot = ScreenshotTaker.takeScreenshot(trackerHandler.driver(), failureOutputDirectory, trackerName, 0);
            LOGGER.warn("\t- Failure screenshot saved at: [{}]", screenshot.getAbsolutePath());
        } catch (final IOException e) {
            LOGGER.debug("\t- Unable to take failure screenshot of '{}'", trackerName, e);
            LOGGER.warn("\t- Unable to take failure screenshot of '{}': {}", trackerName, e.getMessage());
        }
    }

    private static void ensureDirectoryExists(final Path directory) {
        final File directoryHandle = directory.toFile();
        if (!directoryHandle.exists()) {
            LOGGER.trace("Creating directory: '{}'", directoryHandle);
            final boolean wasDirectoryCreated = directoryHandle.mkdirs();
            if (!wasDirectoryCreated) {
                LOGGER.trace("Could not create directory (or already exists): '{}'", directoryHandle);
            }
        }
    }

    private static void screenshotProfile(final AbstractTrackerHandler trackerHandler, final TrackerCredential trackerCredential) throws IOException {
        LOGGER.trace("\t- Starting to take screenshot of profile");
        final Pair<ExistingScreenshotAction, Integer> existingScreenshotValue = doesScreenshotExistAndSkipSelected(trackerCredential.name());
        if (existingScreenshotValue.first() == ExistingScreenshotAction.SKIP) {
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

        if (trackerHandler.hasSensitiveInformation()) {
            LOGGER.info("\t- Redacting elements with sensitive information");
            final int numberOfRedactedElements = trackerHandler.redactElements();
            if (numberOfRedactedElements != 0) {
                LOGGER.info("\t\t- Redacted the text of {} element{}", numberOfRedactedElements, StringUtils.pluralise(numberOfRedactedElements));
            }
        }

        if (trackerHandler.hasFixedHeader()) {
            LOGGER.info("\t- Header has been updated to not be fixed");
        }

        final File screenshot = ScreenshotTaker.takeScreenshot(trackerHandler.driver(), CONFIG.outputDirectory(), trackerCredential.name(),
            existingScreenshotValue.second());
        LOGGER.info("\t- Screenshot saved at: [{}]", screenshot.getAbsolutePath());
        // TODO: Only do this when taking multiple screenshots: trackerHandler.reloadPage();

        trackerHandler.logout();
        LOGGER.info("\t- Logged out");
    }

    private static Pair<ExistingScreenshotAction, Integer> doesScreenshotExistAndSkipSelected(final String trackerName) {
        final int numberOfExistingScreenshots = ScreenshotTaker.howManyScreenshotsAlreadyExist(trackerName);
        // If no screenshots already exist, no need to handle anything
        if (numberOfExistingScreenshots == 0) {
            return Pair.of(ExistingScreenshotAction.OVERWRITE, numberOfExistingScreenshots);
        }

        // Otherwise, we need to decide how to handle the case with an existing screenshot
        return switch (CONFIG.existingScreenshotAction()) {
            case CREATE_ANOTHER -> {
                LOGGER.debug("\t- Screenshot already exists for tracker '{}', taking a new one and appending index to name", trackerName);
                yield Pair.of(ExistingScreenshotAction.CREATE_ANOTHER, numberOfExistingScreenshots);
            }
            case OVERWRITE -> {
                LOGGER.debug("\t- Screenshot already exists for tracker '{}', overwriting with new screenshot", trackerName);
                yield Pair.of(ExistingScreenshotAction.OVERWRITE, 0);
            }
            case SKIP -> {
                LOGGER.warn("\t- Screenshot already exists for tracker '{}', skipping", trackerName);
                yield Pair.of(ExistingScreenshotAction.SKIP, numberOfExistingScreenshots);
            }
        };
    }
}
