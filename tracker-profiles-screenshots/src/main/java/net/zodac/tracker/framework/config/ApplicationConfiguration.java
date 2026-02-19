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

package net.zodac.tracker.framework.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.zodac.tracker.framework.TrackerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: Add ability to enable/disable XXX trackers

/**
 * Utility file that loads the application configuration from environment variables.
 *
 * @param browserDataStoragePath     the file path in which to store browser data (profiles, caches, etc.)
 * @param browserDimensions          the dimensions in the format {@code width,height} for the {@code Selenium} web browser
 * @param csvCommentSymbol           the {@code char} defining a comment row in the CSV file
 * @param enableTranslationToEnglish whether to translate non-English {@link TrackerType}s to English
 * @param inputTimeoutDuration       how long to wait for a user-input (if enabled)
 * @param inputTimeoutEnabled        whether a timeout for a user-input is enabled or not
 * @param forceUiBrowser             whether to use a UI-based browser or not
 * @param outputDirectory            the output {@link Path} to the directory within which the screenshots will be saved
 * @param redactionType              the {@link RedactionType} to perform the redaction of sensitive information on the  user profile page
 * @param existingScreenshotAction   the {@link ExistingScreenshotAction} to perform when a screenshot exists for a tracker
 * @param trackerExecutionOrder      the execution order of the different {@link TrackerType}s
 * @param trackerInputFilePath       the {@link Path} to the input tracker CSV file
 */
public record ApplicationConfiguration(
    String browserDataStoragePath,
    String browserDimensions,
    char csvCommentSymbol,
    boolean enableTranslationToEnglish,
    boolean forceUiBrowser,
    Duration inputTimeoutDuration,
    boolean inputTimeoutEnabled,
    Path outputDirectory,
    RedactionType redactionType,
    ExistingScreenshotAction existingScreenshotAction,
    List<TrackerType> trackerExecutionOrder,
    Path trackerInputFilePath
) {

    private static final Logger LOGGER = LogManager.getLogger();

    // Default values
    private static final String BROWSER_DATA_STORAGE_PATH = "/tmp/chrome-home";
    private static final String DEFAULT_BROWSER_WIDTH = "1680";
    private static final String DEFAULT_BROWSER_HEIGHT = "1050";
    private static final String DEFAULT_CSV_COMMENT_SYMBOL = "#";
    private static final String DEFAULT_OUTPUT_DIRECTORY_NAME_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_OUTPUT_DIRECTORY_PARENT_PATH = "/app/screenshots";
    private static final RedactionType DEFAULT_REDACTION_TYPE = RedactionType.TEXT;
    private static final ExistingScreenshotAction DEFAULT_SCREENSHOT_EXISTS_ACTION = ExistingScreenshotAction.CREATE_ANOTHER;
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_TRACKER_EXECUTION_ORDER = "headless,manual,cloudflare-check";
    private static final String DEFAULT_TRACKER_INPUT_FILE_PATH = DEFAULT_OUTPUT_DIRECTORY_PARENT_PATH + "/trackers.csv";

    private static final Set<String> VALID_RESOLUTIONS = new LinkedHashSet<>(List.of(
        "800x600",
        "1024x768",
        "1280x720",
        "1280x800",
        "1280x1024",
        "1600x1000",
        "1600x1200",
        "1680x1050",
        "1920x1080",
        "1920x1200",
        "2560x1440"
    ));

    /**
     * Loads the {@link ApplicationConfiguration} defined by environment variables.
     *
     * @return the {@link ApplicationConfiguration}
     */
    public static ApplicationConfiguration load() {
        final ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration(
            BROWSER_DATA_STORAGE_PATH,
            getBrowserDimensions(),
            getCsvCommentSymbol(),
            getBooleanEnvironmentVariable("ENABLE_TRANSLATION_TO_ENGLISH", true),
            getBooleanEnvironmentVariable("FORCE_UI_BROWSER", false),
            getInputTimeoutDuration(),
            getBooleanEnvironmentVariable("INPUT_TIMEOUT_ENABLED", false),
            getOutputDirectory(),
            getRedactionType(),
            getScreenshotExistsAction(),
            getTrackerExecutionOrder(),
            getTrackerInputFilePath()
        );

        applicationConfiguration.print();
        return applicationConfiguration;
    }

    private static String getBrowserDimensions() {
        final String browserWidth = getOrDefault("BROWSER_WIDTH", DEFAULT_BROWSER_WIDTH);
        final String browserHeight = getOrDefault("BROWSER_HEIGHT", DEFAULT_BROWSER_HEIGHT);
        final String resolution = String.format("%sx%s", browserWidth, browserHeight);

        if (VALID_RESOLUTIONS.contains(resolution)) {
            return String.format("%s,%s", browserWidth, browserHeight);
        }

        throw new IllegalArgumentException(
            String.format("[BROWSER_WIDTH][BROWSER_HEIGHT] Unsupported browser resolution '%s', must be one of: %s", resolution, VALID_RESOLUTIONS));
    }

    private static char getCsvCommentSymbol() {
        return getOrDefault("CSV_COMMENT_SYMBOL", DEFAULT_CSV_COMMENT_SYMBOL).charAt(0);
    }

    private static Duration getInputTimeoutDuration() {
        final String inputTimeoutSeconds = getOrDefault("INPUT_TIMEOUT_SECONDS", "300");
        final int inputTimeoutSecondsValidated = parsePositiveInteger(inputTimeoutSeconds, "INPUT_TIMEOUT_SECONDS");
        return Duration.ofSeconds(inputTimeoutSecondsValidated);
    }

    private static Path getOutputDirectory() {
        final String timeZone = getOrDefault("TIMEZONE", DEFAULT_TIMEZONE);
        final String outputDirectoryNameFormat = getOrDefault("OUTPUT_DIRECTORY_NAME_FORMAT", DEFAULT_OUTPUT_DIRECTORY_NAME_FORMAT);
        final String outputDirectoryParentPath = getOrDefault("OUTPUT_DIRECTORY_PARENT_PATH", DEFAULT_OUTPUT_DIRECTORY_PARENT_PATH);

        try {
            final LocalDate currentDate = LocalDate.now(ZoneId.of(timeZone));
            final String outputDirectoryName = currentDate.format(DateTimeFormatter.ofPattern(outputDirectoryNameFormat, Locale.getDefault()));
            return Paths.get(outputDirectoryParentPath, outputDirectoryName);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("[OUTPUT_DIRECTORY_NAME_FORMAT] Invalid time format: '%s'", timeZone), e);
        } catch (final DateTimeException e) {
            throw new IllegalArgumentException(String.format("[TIMEZONE] Invalid timezone: '%s'", timeZone), e);
        }
    }

    private static RedactionType getRedactionType() {
        final String redactionTypeRaw = getOrDefault("REDACTION_TYPE", DEFAULT_REDACTION_TYPE.toString());
        final RedactionType redactionTypeInput = RedactionType.get(redactionTypeRaw);
        if (redactionTypeInput == null) {
            throw new IllegalArgumentException(String.format("[REDACTION_TYPE] Invalid value: '%s'", redactionTypeRaw));
        }

        return redactionTypeInput;
    }

    private static ExistingScreenshotAction getScreenshotExistsAction() {
        final String existingScreenshotActionRaw = getOrDefault("SCREENSHOT_EXISTS_ACTION", DEFAULT_SCREENSHOT_EXISTS_ACTION.toString());
        final ExistingScreenshotAction existingScreenshotActionInput = ExistingScreenshotAction.get(existingScreenshotActionRaw);
        if (existingScreenshotActionInput == null) {
            throw new IllegalArgumentException(String.format("[SCREENSHOT_EXISTS_ACTION] Invalid value: '%s'", existingScreenshotActionRaw));
        }

        return existingScreenshotActionInput;
    }

    private static List<TrackerType> getTrackerExecutionOrder() {
        final String executionOrderRaw = getOrDefault("TRACKER_EXECUTION_ORDER", DEFAULT_TRACKER_EXECUTION_ORDER);
        final String[] executionOrderTokens = executionOrderRaw.split(",");
        if (executionOrderTokens.length == 0 || executionOrderTokens.length > TrackerType.ALL_VALUES.size()) {
            throw new IllegalArgumentException(
                String.format("[TRACKER_EXECUTION_ORDER] 1-%d tracker types required, found: %s", TrackerType.ALL_VALUES.size(),
                    Arrays.toString(executionOrderTokens)));
        }

        final Collection<TrackerType> trackerExecutionOrder = new LinkedHashSet<>();
        for (final String executionOrderToken : executionOrderTokens) {
            final TrackerType trackerType = TrackerType.find(executionOrderToken);
            if (trackerType == null) {
                throw new IllegalArgumentException(String.format("[TRACKER_EXECUTION_ORDER] Invalid tracker found: '%s'", executionOrderToken));
            }

            if (!trackerExecutionOrder.add(trackerType)) {
                throw new IllegalArgumentException(String.format("[TRACKER_EXECUTION_ORDER] Duplicate tracker found: '%s'", executionOrderToken));
            }
        }

        return List.copyOf(trackerExecutionOrder);
    }

    private static Path getTrackerInputFilePath() {
        return Paths.get(getOrDefault("TRACKER_INPUT_FILE_PATH", DEFAULT_TRACKER_INPUT_FILE_PATH));
    }

    private static boolean getBooleanEnvironmentVariable(final String environmentVariableName, final boolean defaultValue) {
        return Boolean.parseBoolean(getOrDefault(environmentVariableName, Boolean.toString(defaultValue)));
    }

    private static int parsePositiveInteger(final String input, final String environmentVariableName) {
        try {
            final int integer = Integer.parseInt(input);
            if (integer <= 0) {
                throw new IllegalArgumentException(String.format("[%s] Invalid input '%s', must be greater than 0", environmentVariableName, input));
            }
            return integer;
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(String.format("[%s] Invalid input '%s', must be a valid number", environmentVariableName, input), e);
        }
    }

    private static String getOrDefault(final String environmentVariableName, final String defaultValue) {
        final String environmentVariable = System.getenv(environmentVariableName);
        if (environmentVariable != null) {
            return environmentVariable;
        }
        return defaultValue;
    }

    private void print() {
        LOGGER.debug("Loaded application configuration:");
        LOGGER.debug("\t- browserDataStoragePath={}", browserDataStoragePath);
        LOGGER.debug("\t- browserDimensions={}", browserDimensions);
        LOGGER.debug("\t- csvCommentSymbol={}", csvCommentSymbol);
        LOGGER.debug("\t- enableTranslationToEnglish={}", enableTranslationToEnglish);
        LOGGER.debug("\t- forceUiBrowser={}", forceUiBrowser);
        LOGGER.debug("\t- outputDirectory={}", outputDirectory);
        LOGGER.debug("\t- existingScreenshotAction={}", existingScreenshotAction);
        LOGGER.debug("\t- trackerExecutionOrder={}", trackerExecutionOrder);
        LOGGER.debug("\t- trackerInputFilePath={}", trackerInputFilePath);
    }
}
