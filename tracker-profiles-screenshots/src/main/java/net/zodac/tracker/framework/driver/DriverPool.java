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

package net.zodac.tracker.framework.driver;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.driver.extension.Extension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Singleton pool that manages {@link RemoteWebDriver} instances for tracker execution.
 *
 * <p>
 * Trackers that do not require browser extensions share a single pooled driver per {@link TrackerType}, since extensions must be installed at browser
 * startup and cannot be added to an already-running browser. Trackers using extensions receive a fresh driver on each call to
 * {@link #acquire(TrackerType, List)}.
 */
public final class DriverPool {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DEFAULT_BROWSER_PAGE = "about:blank";

    private final Map<TrackerType, RemoteWebDriver> pool = new EnumMap<>(TrackerType.class);

    private DriverPool() {

    }

    private static final class InstanceHolder {
        private static final DriverPool INSTANCE = new DriverPool();
    }

    private static DriverPool get() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Acquires a {@link RemoteWebDriver} for a tracker.
     *
     * <p>
     * If {@code extensions} is non-empty, a fresh driver is created with those {@link Extension}s installed at browser startup, then each
     * {@link Extension#configure(RemoteWebDriver)} is called. This driver is not pooled and will be quit on {@link #release(RemoteWebDriver)}.
     *
     * <p>
     * If {@code extensions} is empty, the pooled driver for the given {@link TrackerType} is returned, creating one if it does not yet exist. This
     * driver will be returned to the pool on {@link #release(RemoteWebDriver)}.
     *
     * @param trackerType the {@link TrackerType}
     * @param extensions  the {@link Extension}s to install, if any
     * @return the {@link RemoteWebDriver}
     */
    public static RemoteWebDriver acquire(final TrackerType trackerType, final List<Extension> extensions) {
        if (!extensions.isEmpty()) {
            LOGGER.trace("Creating fresh driver for extension-based {} tracker", trackerType);
            final RemoteWebDriver driver = JavaWebDriverFactory.createDriver(trackerType, extensions);
            for (final Extension extension : extensions) {
                LOGGER.trace("Configuring extension {}", extension.getClass().getSimpleName());
                extension.configure(driver);
            }
            return driver;
        }

        LOGGER.trace("Acquiring pooled driver for type {}", trackerType);
        return get().pool.computeIfAbsent(trackerType, type -> {
            LOGGER.info("Creating pooled {} driver", type.formattedName());
            return JavaWebDriverFactory.createDriver(type, List.of());
        });
    }

    /**
     * Releases a {@link RemoteWebDriver} back to the pool.
     *
     * <p>
     * Pooled drivers are navigated to {@link #DEFAULT_BROWSER_PAGE} to clear the current page state without quitting the browser. Drivers not
     * originally from the pool are simply {@link RemoteWebDriver#quit()}.
     *
     * @param driver the {@link RemoteWebDriver} to release
     */
    public static void release(final RemoteWebDriver driver) {
        if (get().pool.containsValue(driver)) {
            LOGGER.trace("Returning pooled driver, navigating to blank page");
            try {
                driver.navigate().to(DEFAULT_BROWSER_PAGE);
            } catch (final Exception e) {
                LOGGER.trace("Unable to navigate pooled driver to blank page", e);
            }
        } else {
            LOGGER.trace("Quitting fresh driver");
            try {
                driver.quit();
            } catch (final Exception e) {
                LOGGER.trace("Unable to quit fresh driver", e);
            }
        }
    }

    /**
     * Shuts down the pool, quitting all pooled {@link RemoteWebDriver} instances and clearing the pool.
     */
    public static void shutdown() {
        final DriverPool instance = get();
        LOGGER.debug("Shutting down driver pool with {} pooled driver(s)", instance.pool.size());
        for (final RemoteWebDriver driver : instance.pool.values()) {
            try {
                driver.quit();
            } catch (final Exception e) {
                LOGGER.trace("Error quitting pooled driver during shutdown", e);
            }
        }
        instance.pool.clear();
    }
}
