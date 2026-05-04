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

import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.driver.extension.Extension;
import net.zodac.tracker.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Singleton pool that manages {@link RemoteWebDriver} instances for tracker execution.
 *
 * <p>
 * Trackers that do not require browser extensions share a pool of drivers per {@link TrackerType}. For
 * {@link TrackerType#HEADLESS} types the pool size equals the configured thread count (see
 * {@link #initialise(TrackerType, int)}); for all other types the pool holds exactly one driver. Trackers that implement
 * {@link net.zodac.tracker.handler.definition.UsesExtensions} always receive a fresh driver on each call to
 * {@link #acquire(TrackerType, List)}.
 *
 * <p>
 * Call {@link #initialise(TrackerType, int)} once per {@link TrackerType} before execution of that type begins to pre-create its pooled drivers.
 * Callers then acquire a driver via {@link #acquire(TrackerType, List)} — blocking if no driver is free — use it, and return
 * it via {@link #release(RemoteWebDriver)}. Call {@link #shutdown()} once all work has completed.
 */
public final class DriverPool {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DEFAULT_BROWSER_PAGE = "about:blank";

    private final Map<TrackerType, BlockingDeque<RemoteWebDriver>> pool = new EnumMap<>(TrackerType.class);
    private final Set<RemoteWebDriver> allPooledDrivers = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<RemoteWebDriver, TrackerType> driverTypeMap = new IdentityHashMap<>();

    private DriverPool() {

    }

    private static final class InstanceHolder {
        private static final DriverPool INSTANCE = new DriverPool();
    }

    private static DriverPool get() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Pre-creates pooled {@link RemoteWebDriver} instances for the provided {@link TrackerType}.
     *
     * <p>
     * For {@link TrackerType#HEADLESS}, {@code numberOfParallelThreads} drivers are created and added to the pool. For all other types,
     * exactly one driver is created. Each driver is registered so that {@link #release(RemoteWebDriver)} can return it correctly.
     *
     * <p>
     * This method must be called once per {@link TrackerType}, before any call to {@link #acquire(TrackerType, List)} for that type.
     *
     * @param trackerType             the {@link TrackerType} to initialise
     * @param numberOfParallelThreads the number of pooled drivers to create for {@link TrackerType#HEADLESS}
     */
    public static void initialise(final TrackerType trackerType, final int numberOfParallelThreads) {
        final DriverPool instance = get();
        final int count = (trackerType == TrackerType.HEADLESS) ? numberOfParallelThreads : 1;
        LOGGER.debug("Initializing {} pooled {} driver{}", count, trackerType.formattedName(), StringUtils.pluralise(count));

        final BlockingDeque<RemoteWebDriver> deque = new LinkedBlockingDeque<>();
        for (int i = 0; i < count; i++) {
            final RemoteWebDriver driver = JavaWebDriverFactory.createDriver(trackerType, List.of());
            deque.addLast(driver);
            instance.allPooledDrivers.add(driver);
            instance.driverTypeMap.put(driver, trackerType);
        }
        instance.pool.put(trackerType, deque);
    }

    /**
     * Acquires a {@link RemoteWebDriver} for a tracker.
     *
     * <p>
     * If {@code extensions} is non-empty, a fresh driver is created with those {@link Extension}s installed at browser startup, then each
     * {@link Extension#configure(RemoteWebDriver)} is called. This driver is not pooled and will be quit on {@link #release(RemoteWebDriver)}.
     *
     * <p>
     * If {@code extensions} is empty, the pooled driver for the given {@link TrackerType} is returned, blocking until one is available. The
     * driver will be returned to the pool on {@link #release(RemoteWebDriver)}.
     *
     * @param trackerType the {@link TrackerType} controlling headless vs. UI execution
     * @param extensions  the {@link Extension}s to install and configure; an empty list requests a pooled driver
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

        final DriverPool instance = get();
        final BlockingDeque<RemoteWebDriver> deque = instance.pool.get(trackerType);
        if (deque != null) {
            LOGGER.trace("Acquiring pooled driver for type {}", trackerType);
            try {
                return deque.takeFirst();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for pooled driver for type %s".formatted(trackerType), e);
            }
        }

        // Fallback: initialize() was not called — create a single driver lazily
        LOGGER.debug("No pool initialized for type {}, creating driver lazily", trackerType);
        final RemoteWebDriver lazyDriver = JavaWebDriverFactory.createDriver(trackerType, List.of());
        final BlockingDeque<RemoteWebDriver> newDeque = new LinkedBlockingDeque<>();
        instance.allPooledDrivers.add(lazyDriver);
        instance.driverTypeMap.put(lazyDriver, trackerType);
        instance.pool.put(trackerType, newDeque);
        return lazyDriver;
    }

    /**
     * Releases a {@link RemoteWebDriver} back to the pool.
     *
     * <p>
     * Pooled drivers are navigated to {@link #DEFAULT_BROWSER_PAGE} to clear the current page state without quitting the browser, then
     * returned to the pool for the next caller. Drivers not originally from the pool are simply {@link RemoteWebDriver#quit()}.
     *
     * @param driver the {@link RemoteWebDriver} to release
     */
    public static void release(final RemoteWebDriver driver) {
        final DriverPool instance = get();
        if (instance.allPooledDrivers.contains(driver)) {
            LOGGER.trace("Returning pooled driver, navigating to blank page");
            try {
                driver.navigate().to(DEFAULT_BROWSER_PAGE);
            } catch (final Exception e) {
                LOGGER.trace("Unable to navigate pooled driver to blank page", e);
            }
            returnDriverToPool(instance, driver);
        } else {
            LOGGER.trace("Quitting fresh driver");
            try {
                driver.quit();
            } catch (final Exception e) {
                LOGGER.trace("Unable to quit fresh driver", e);
            }
        }
    }

    private static void returnDriverToPool(final DriverPool instance, final RemoteWebDriver driver) {
        final TrackerType type = instance.driverTypeMap.get(driver);
        if (type == null) {
            return;
        }
        final BlockingDeque<RemoteWebDriver> deque = instance.pool.get(type);
        if (deque != null) {
            deque.addFirst(driver);
        }
    }

    /**
     * Shuts down the pool, quitting all pooled {@link RemoteWebDriver} instances and clearing all tracking state.
     */
    public static void shutdown() {
        final DriverPool instance = get();
        LOGGER.debug("Shutting down driver pool with {} pooled driver(s)", instance.allPooledDrivers.size());
        for (final RemoteWebDriver driver : instance.allPooledDrivers) {
            try {
                driver.quit();
            } catch (final Exception e) {
                LOGGER.trace("Error quitting pooled driver during shutdown", e);
            }
        }
        instance.pool.clear();
        instance.allPooledDrivers.clear();
        instance.driverTypeMap.clear();
    }
}
