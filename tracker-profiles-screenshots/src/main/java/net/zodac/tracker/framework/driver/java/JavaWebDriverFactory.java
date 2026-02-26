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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import net.zodac.tracker.framework.driver.extension.Extension;
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
     * @param trackerType whether {@link TrackerType} defining the execution method for this tracker
     * @param extensions  any {@link Extension}s to be installed
     * @return the {@link RemoteWebDriver} instance
     */
    public static RemoteWebDriver createDriver(final TrackerType trackerType, final Collection<Extension> extensions) {
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

        for (final Extension extension : extensions) {
            LOGGER.trace("Installing extension {} from '{}'", extension.getClass().getSimpleName(), extension.path());
            final File extensionFile = new File(extension.path());
            chromeOptions.addExtensions(extensionFile);
        }

        LOGGER.trace("Creating driver with following options: {}", chromeOptions);
        return new ChromeDriver(chromeOptions);
    }

    private static Map<String, String> getTranslationWhitelist() {
        final String[] baseLanguages = {
            "af", "am", "ar", "az", "be", "bg", "bn", "bs", "ca", "co",
            "cs", "cy", "da", "de", "el", "eo", "es", "et", "eu", "fa",
            "fi", "fr", "fy", "ga", "gd", "gl", "gu", "ha", "he", "hi",
            "hr", "hu", "hy", "id", "ig", "is", "it", "iw", "ja", "jw",
            "ka", "kk", "km", "kn", "ko", "ku", "ky", "la", "lb", "lo",
            "lt", "lv", "mg", "mi", "mk", "ml", "mn", "mr", "ms", "mt",
            "my", "ne", "nl", "no", "ny", "pa", "pl", "ps", "pt", "ro",
            "ru", "sd", "si", "sk", "sl", "sm", "sn", "so", "sq", "sr",
            "st", "su", "sv", "sw", "ta", "te", "tg", "th", "tr", "uk",
            "ur", "uz", "vi", "xh", "yi", "yo", "zh", "zu"
        };

        final Map<String, String> translateWhitelists = new HashMap<>(baseLanguages.length);
        for (final String baseLanguage : baseLanguages) {
            translateWhitelists.put(baseLanguage, "en");
        }
        return translateWhitelists;
    }

    private static boolean canTrackerUseHeadlessBrowser(final TrackerType trackerType) {
        return !(CONFIG.forceUiBrowser() || trackerType == TrackerType.MANUAL);
    }
}
