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

package net.zodac.tracker.framework.driver.java;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Factory class used to create an instance of a {@link RemoteWebDriver}.
 */
public final class JavaWebDriverFactory {

    private static final ApplicationConfiguration CONFIG = Configuration.get();
    private static final Logger LOGGER = LogManager.getLogger();

    private JavaWebDriverFactory() {

    }

    /**
     * Creates an instance of {@link RemoteWebDriver} to be used by the {@link net.zodac.tracker.handler.AbstractTrackerHandler}. This is a standard
     * Java Selenium {@link RemoteWebDriver}, with some arguments to configure it for simpler automation.
     *
     * <p>
     * This {@link RemoteWebDriver} may run in headless mode, if the following conditions are met:
     *
     * <ul>
     *     <li>{@link ApplicationConfiguration#forceUiBrowser()} is not {@code true}</li>
     *     <li>{@link TrackerType} is not {@link TrackerType#MANUAL}</li>
     * </ul>
     *
     * <p>
     * Otherwise it will run in full UI mode.
     *
     * @param trackerType whether {@link TrackerType} defining the execution method for this tracker.
     * @return the {@link RemoteWebDriver} instance
     */
    public static RemoteWebDriver createDriver(final TrackerType trackerType) {
        LOGGER.trace("Creating Java driver");
        final ChromeOptions chromeOptions = new ChromeOptions();

        // User-defined options
        chromeOptions.addArguments("--window-size=" + CONFIG.browserDimensions());
        if (canTrackerUseHeadlessBrowser(trackerType)) {
            chromeOptions.addArguments("--headless=new");
            chromeOptions.addArguments("--start-maximized");
        }

        // Cache to avoid reloading data on subsequent runs
        chromeOptions.addArguments("--disk-cache-dir=" + CONFIG.browserDataStoragePath() + File.separator + "selenium");

        // Following 3 options are to ensure there are no conflicting issues running the browser on Linux
        chromeOptions.addArguments("--user-data-dir=" + CONFIG.browserDataStoragePath() + File.separator + System.nanoTime());
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");

        final Map<String, Object> driverPreferences = new HashMap<>();
        // Disable password manager pop-ups
        driverPreferences.put("credentials_enable_service", false);
        driverPreferences.put("profile.password_manager_enabled", false);

        if (CONFIG.enableTranslationToEnglish()) {
            // Translation options
            chromeOptions.addArguments("--lang=en");
            driverPreferences.put("intl.accept_languages", "en,en_US");
            driverPreferences.put("translate_accepted_count", 1);
            driverPreferences.put("translate.enabled", true);
            driverPreferences.put("translate_whitelists", getTranslationWhitelist());
        }

        chromeOptions.setExperimentalOption("prefs", driverPreferences);

        // Additional flags to remove unnecessary information on browser
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-notifications");
        chromeOptions.addArguments("--ignore-certificate-errors");

        LOGGER.debug("Creating driver with following options: {}", chromeOptions);
        return new ChromeDriver(chromeOptions);
    }

    private static Map<String, String> getTranslationWhitelist() {
        final Map<String, String> translateWhitelists = new HashMap<>();

        // Base languages
        final String[] baseLanguages = {
            "af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs",
            "bg", "ca", "ceb", "ny", "zh", "co", "hr", "cs",
            "da", "nl", "eo", "et", "tl", "fi", "fr", "fy", "gl", "ka",
            "de", "el", "gu", "ht", "ha", "haw", "he", "iw", "hi", "hmn",
            "hu", "is", "ig", "id", "ga", "it", "ja", "jw", "kn", "kk",
            "km", "ko", "ku", "ky", "lo", "la", "lv", "lt", "lb", "mk",
            "mg", "ms", "ml", "mt", "mi", "mr", "mn", "my", "ne", "no",
            "ps", "fa", "pl", "pt", "pa", "ro", "ru", "sm", "gd", "sr",
            "st", "sn", "sd", "si", "sk", "sl", "so", "es", "su", "sw",
            "sv", "tg", "ta", "te", "th", "tr", "uk", "ur", "uz", "vi",
            "cy", "xh", "yi", "yo", "zu"
        };

        for (final String baseLanguage : baseLanguages) {
            translateWhitelists.put(baseLanguage, "en");
        }
        return translateWhitelists;
    }

    private static boolean canTrackerUseHeadlessBrowser(final TrackerType trackerType) {
        return !(CONFIG.forceUiBrowser() || trackerType == TrackerType.MANUAL);
    }
}
