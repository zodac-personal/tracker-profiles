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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import net.zodac.tracker.framework.TrackerCredential;
import net.zodac.tracker.framework.TrackerHandlerFactory;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import net.zodac.tracker.framework.config.ExistingScreenshotAction;
import net.zodac.tracker.framework.config.RedactionType;
import net.zodac.tracker.framework.exception.CancelledInputException;
import net.zodac.tracker.framework.exception.DriverAttachException;
import net.zodac.tracker.framework.exception.NoUserInputException;
import net.zodac.tracker.framework.exception.TranslationException;
import net.zodac.tracker.handler.AbstractTrackerHandler;
import net.zodac.tracker.handler.definition.DoesNotScrollDuringScreenshot;
import net.zodac.tracker.handler.definition.HasDismissibleBanner;
import net.zodac.tracker.handler.definition.HasFixedHeader;
import net.zodac.tracker.handler.definition.HasFixedSidebar;
import net.zodac.tracker.handler.definition.HasJumpButtons;
import net.zodac.tracker.handler.definition.NeedsExplicitTranslation;
import net.zodac.tracker.redaction.Redactor;
import net.zodac.tracker.redaction.RedactorImpl;
import net.zodac.tracker.util.BrowserInteractionHelper;
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

    private static final int FIRST_ATTEMPT = 1;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ApplicationConfiguration CONFIG = Configuration.get();
    private static final Path ERRORS_DIRECTORY = CONFIG.outputDirectory().resolve("errors");

    private ProfileScreenshotExecutor() {

    }

    /**
     * Attempts to take a screenshot for the given {@link TrackerCredential}.
     *
     * @param trackerCredential details of the tracker to screenshot
     * @return {@code true} if screenshot was successful
     * @throws RuntimeException thrown if all attempts are exhausted due to a retryable failure with a known cause
     */
    static boolean canScreenshotTracker(final TrackerCredential trackerCredential) {
        LOGGER.info("");
        LOGGER.info("[{}]", trackerCredential.name());

        final int maxAttempts = CONFIG.numberOfTrackerAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (attempt != FIRST_ATTEMPT) {
                LOGGER.warn("");
                LOGGER.warn("[{}] (attempt {}/{})", trackerCredential.name(), attempt, maxAttempts);
            }

            try {
                final boolean screenshotResult = isSuccessfullyScreenshot(trackerCredential);

                if (screenshotResult) {
                    LOGGER.trace("Successfully screenshot '{}' on attempt #{}", trackerCredential.name(), attempt);
                    clearErrorScreenshots(trackerCredential.name());
                    return true;
                }
            } catch (final Exception e) {
                LOGGER.trace("Error screenshotting '{}' on attempt #{}", trackerCredential.name(), attempt, e);
                BrowserInteractionHelper.explicitWait(Duration.ofSeconds(1L), "a moment before retrying");
            }
        }

        LOGGER.debug("\t- All {} attempts exhausted for tracker '{}'", maxAttempts, trackerCredential.name());
        return false;
    }

    private static boolean isSuccessfullyScreenshot(final TrackerCredential trackerCredential) {
        AbstractTrackerHandler trackerHandler = null;
        try { // NOPMD: UseTryWithResources - need access to the trackerHandler to take a screenshot on error
            trackerHandler = TrackerHandlerFactory.getHandler(trackerCredential.name());
            screenshotProfile(trackerHandler, trackerCredential);
            return true;
        } catch (final CancelledInputException e) {
            LOGGER.debug("\t- User cancelled manual input for tracker '{}'", trackerCredential.name(), e);
            LOGGER.warn("\t- User cancelled manual input for tracker '{}'", trackerCredential.name());
        } catch (final DriverAttachException e) {
            LOGGER.debug("\t- Unable to attach to Python Selenium web browser for tracker '{}'", trackerCredential.name(), e);
            LOGGER.warn("\t- Unable to attach to Python Selenium web browser for tracker '{}'", trackerCredential.name());
        } catch (final FileNotFoundException e) {
            LOGGER.debug("\t- Unable to find expected file for tracker '{}'", trackerCredential.name());
            LOGGER.warn("\t- Unable to find expected file for tracker '{}': {}", trackerCredential.name(), e.getMessage());
        } catch (final NoSuchElementException e) {
            LOGGER.debug("\t- No implementation for tracker '{}'", trackerCredential.name(), e);
            LOGGER.warn("\t- No implementation for tracker '{}'", trackerCredential.name());
        } catch (final NoUserInputException e) {
            LOGGER.debug("\t- User provided no manual input for tracker '{}'", trackerCredential.name(), e);
            LOGGER.warn("\t- User provided no manual input for tracker '{}'", trackerCredential.name());
        } catch (final TimeoutException e) {
            screenshotOnError(trackerHandler, trackerCredential.name());
            LOGGER.debug("\t- Timed out waiting to find required element for tracker '{}'", trackerCredential.name(), e);

            final String cleanedErrorMessage = StringUtils.firstLine(e.getMessage());
            if (cleanedErrorMessage.isEmpty()) {
                LOGGER.warn("\t- Timed out waiting to find required element for tracker '{}'", trackerCredential.name());
            } else {
                LOGGER.warn("\t- Timed out waiting to find required element for tracker '{}': {}", trackerCredential.name(), cleanedErrorMessage);
            }
        } catch (final TranslationException e) {
            LOGGER.debug("\t- Unable to translate tracker '{}' to English", trackerCredential.name(), e);
            LOGGER.warn("\t- Unable to translate tracker '{}' to English: {}", trackerCredential.name(), e.getMessage());
        } catch (final NoSuchSessionException | NoSuchWindowException | UnreachableBrowserException e) {
            LOGGER.debug("Browser unavailable, most likely user-cancelled", e);
        } catch (final Exception e) {
            screenshotOnError(trackerHandler, trackerCredential.name());
            LOGGER.debug("\t- Unexpected error taking screenshot of '{}'", trackerCredential.name(), e);

            final String cleanedErrorMessage = StringUtils.firstLine(e.getMessage());
            if (cleanedErrorMessage.isEmpty()) {
                LOGGER.warn("\t- Unexpected error taking screenshot of '{}'", trackerCredential.name());
            } else {
                LOGGER.warn("\t- Unexpected error taking screenshot of '{}': {}", trackerCredential.name(), cleanedErrorMessage);
            }
        } finally {
            if (trackerHandler != null) {
                trackerHandler.close();
            }
        }

        return false;
    }

    private static void clearErrorScreenshots(final String trackerName) {
        final File errorsDirectory = ERRORS_DIRECTORY.toFile();
        if (!errorsDirectory.exists()) {
            return;
        }

        final File[] errorScreenshots = errorsDirectory.listFiles((_, name) -> name.startsWith(trackerName));
        if (errorScreenshots != null) {
            for (final File errorScreenshot : errorScreenshots) {
                try {
                    Files.delete(errorScreenshot.toPath());
                    LOGGER.debug("\t- Deleted error screenshot after successful retry: [{}]", errorScreenshot.getAbsolutePath());
                } catch (final IOException e) {
                    LOGGER.warn("\t- Failed to delete error screenshot: [{}]", errorScreenshot.getAbsolutePath(), e);
                }
            }
        }

        final String[] remaining = errorsDirectory.list();
        if (remaining != null && remaining.length == 0) {
            LOGGER.trace("No error screenshots remaining, deleting directory");
            try {
                Files.delete(errorsDirectory.toPath());
                LOGGER.debug("\t- Deleted empty errors directory after successful retry: [{}]", errorsDirectory.getAbsolutePath());
            } catch (final IOException e) {
                LOGGER.warn("\t- Failed to delete errors screenshot: [{}]", errorsDirectory.getAbsolutePath(), e);
            }
        }
    }

    private static void screenshotOnError(final @Nullable AbstractTrackerHandler trackerHandler, final String trackerName) {
        if (!CONFIG.takeScreenshotOnError()) {
            LOGGER.trace("Error occurred, but screenshots for errors are not enabled");
            return;
        }

        if (trackerHandler == null) {
            LOGGER.warn("\t\t- Unable to take failure screenshot, trackerHandler is unexpectedly null");
            return;
        }

        try {
            LOGGER.trace("Taking failure screenshot for '{}'", trackerName);
            ensureErrorDirectoryExists();
            final File screenshot = ScreenshotTaker.takeScreenshot(trackerHandler.driver(), ERRORS_DIRECTORY, trackerName, true, 0);
            LOGGER.warn("\t\t- Failure screenshot saved at: [{}]", screenshot.getAbsolutePath());
        } catch (final IOException e) {
            LOGGER.debug("\t\t- Unable to take failure screenshot of '{}'", trackerName, e);
            LOGGER.warn("\t\t- Unable to take failure screenshot of '{}': {}", trackerName, e.getMessage());
        }
    }

    private static void ensureErrorDirectoryExists() {
        final File directoryHandle = ERRORS_DIRECTORY.toFile();
        if (!directoryHandle.exists()) {
            LOGGER.trace("Creating directory: '{}'", directoryHandle);
            final boolean created = directoryHandle.mkdirs();
            if (!created) {
                LOGGER.trace("Could not create directory (or already exists): '{}'", directoryHandle);
            }
        }
    }

    private static void screenshotProfile(final AbstractTrackerHandler trackerHandler, final TrackerCredential trackerCredential) throws IOException {
        LOGGER.trace("\t- Starting to take screenshot of profile");
        final List<RedactionType> redactionsToExecute = redactionTypesToExecute(trackerCredential.name(), CONFIG.redactionTypes());
        if (redactionsToExecute.isEmpty()) {
            LOGGER.warn("\t- Screenshots already exist for all redaction types for tracker '{}', skipping", trackerCredential.name());
            return;
        }

        if (redactionsToExecute.size() != CONFIG.redactionTypes().size()) {
            LOGGER.warn("\t- Some screenshots already exist for tracker '{}', only executing: {}", trackerCredential.name(), redactionsToExecute);
        }

        LOGGER.info("\t- Opening tracker");
        trackerHandler.openTracker();
        trackerHandler.navigateToLoginPage(trackerCredential.name());

        LOGGER.info("\t- Logging in as '{}'", trackerCredential.username());
        trackerHandler.login(trackerCredential.username(), trackerCredential.password(), trackerCredential.name());

        if (trackerHandler instanceof HasDismissibleBanner trackerWithBanner) {
            trackerWithBanner.dismissBanner();
            LOGGER.info("\t- Banner has been cleared");
        }

        LOGGER.info("\t- Opening user profile page");
        trackerHandler.openProfilePage();

        final boolean scrollDuringScreenshot = !(trackerHandler instanceof DoesNotScrollDuringScreenshot);

        // If the tracker has no sensitive information, all redaction types produce identical screenshots, so we take a single one
        final List<RedactionType> effectiveRedactions;
        if (trackerHandler.hasSensitiveInformation()) {
            effectiveRedactions = redactionsToExecute;
        } else {
            LOGGER.debug("\t- No sensitive information to redact, taking a single screenshot");
            effectiveRedactions = redactionTypesToExecute(trackerCredential.name(), Set.of(RedactionType.NONE));
            if (effectiveRedactions.isEmpty()) {
                LOGGER.warn("\t- Screenshot already exists for tracker '{}' with no sensitive information, skipping", trackerCredential.name());
            }
        }

        for (int i = 0; i < effectiveRedactions.size(); i++) {
            if (i > 0) {
                // Reload to restore page to original state (clears DOM mutations from previous redaction) before next redaction
                LOGGER.debug("\t\t- Reloading profile page for next redaction");
                trackerHandler.reloadProfilePage();
            }

            final RedactionType redactionType = effectiveRedactions.get(i);
            takeScreenshotForRedactionType(trackerHandler, trackerCredential, redactionType, scrollDuringScreenshot);
        }

        trackerHandler.logout();
        LOGGER.info("\t- Logged out");
    }

    private static void takeScreenshotForRedactionType(final AbstractTrackerHandler trackerHandler,
                                                       final TrackerCredential trackerCredential,
                                                       final RedactionType redactionType,
                                                       final boolean scrollDuringScreenshot
    ) throws IOException {
        LOGGER.info("\t- Redaction: {}", redactionType.formattedName());
        final String baseName = screenshotBaseName(trackerCredential.name(), redactionType);

        updatingProfilePage(trackerHandler);

        if (redactionType == RedactionType.NONE) {
            LOGGER.debug("\t\t- Not redacting content");
        } else if (trackerHandler.hasSensitiveInformation()) {
            final Redactor redactor = new RedactorImpl(trackerHandler.driver(), redactionType);
            LOGGER.info("\t\t- Redacting elements with sensitive information");

            final int numberOfRedactedElements = trackerHandler.redactElements(redactor);
            if (numberOfRedactedElements == 0) {
                LOGGER.warn("\t\t- Unexpectedly found nothing to redact");
            } else {
                LOGGER.info("\t\t- Redacted the text of {} element{}", numberOfRedactedElements, StringUtils.pluralise(numberOfRedactedElements));
            }
        } else {
            LOGGER.debug("\t\t- Nothing to redact");
        }

        trackerHandler.actionBeforeScreenshot();
        final File screenshot = ScreenshotTaker.takeScreenshot(trackerHandler.driver(), CONFIG.outputDirectory(), baseName, scrollDuringScreenshot,
            screenshotIndex(baseName));
        trackerHandler.actionAfterScreenshot();
        LOGGER.info("\t\t- Screenshot saved at: [{}]", screenshot.getAbsolutePath());

        // TODO: Undo redaction here, instead of reloading the page in the calling message?
    }

    // Perform modifications to the user profile page before redaction so redaction positions are computed against the settled layout
    private static void updatingProfilePage(final AbstractTrackerHandler trackerHandler) {
        LOGGER.info("\t\t- Performing updates to profile page, if needed");

        if (CONFIG.enableTranslationToEnglish() && trackerHandler instanceof NeedsExplicitTranslation trackerNeedsTranslation) {
            LOGGER.info("\t\t\t- Translating profile page to English");
            trackerNeedsTranslation.translatePageToEnglish();
        }

        if (trackerHandler instanceof HasFixedHeader trackerWithFixedHeader) {
            LOGGER.debug("\t\t\t- Unfixing header");
            trackerWithFixedHeader.unfixHeader(trackerHandler.driver(), trackerWithFixedHeader.headerSelector());
            LOGGER.info("\t\t\t- Header has been updated to not be fixed");
        }

        if (trackerHandler instanceof HasFixedSidebar trackerWithFixedSidebar) {
            LOGGER.debug("\t\t\t- Unfixing sidebar");
            trackerWithFixedSidebar.unfixSidebar(trackerHandler.driver());
            LOGGER.info("\t\t\t- Sidebar has been updated to not be fixed");
        }

        if (trackerHandler instanceof HasJumpButtons trackerWithJumpButtons) {
            LOGGER.debug("\t\t\t- Hiding jump to top/bottom buttons");
            trackerWithJumpButtons.hideJumpButtons(trackerHandler.driver(), trackerWithJumpButtons.jumpButtonSelectors());
            LOGGER.info("\t\t\t- Top/bottom jump buttons have been hidden");
        }
    }

    private static List<RedactionType> redactionTypesToExecute(final String trackerName, final Set<RedactionType> redactionTypes) {
        if (CONFIG.existingScreenshotAction() != ExistingScreenshotAction.SKIP) {
            return redactionTypes.stream().toList();
        }

        // If ExistingScreenshotAction == SKIP, we need to verify if any screenshots have already been taken and exclude those from being taken again
        final List<RedactionType> typesToProcess = new ArrayList<>();
        for (final RedactionType type : redactionTypes) {
            final String baseName = screenshotBaseName(trackerName, type);
            if (ScreenshotTaker.howManyScreenshotsAlreadyExist(baseName, CONFIG.outputDirectory()) == 0) {
                typesToProcess.add(type);
            } else {
                LOGGER.debug("\t- Screenshot already exists for '{}', skipping", baseName);
            }
        }
        return typesToProcess;
    }

    private static int screenshotIndex(final String baseName) {
        return CONFIG.existingScreenshotAction() == ExistingScreenshotAction.CREATE_ANOTHER
            ? ScreenshotTaker.howManyScreenshotsAlreadyExist(baseName, CONFIG.outputDirectory())
            : 0;
    }

    private static String screenshotBaseName(final String trackerName, final RedactionType redactionType) {
        return redactionType == RedactionType.NONE ? trackerName : (trackerName + "_" + redactionType.formattedName());
    }
}
