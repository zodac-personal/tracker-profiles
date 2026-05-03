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

import java.util.Map;
import java.util.Set;
import net.zodac.tracker.app.ScreenshotOrchestrator;
import net.zodac.tracker.app.TrackerRetriever;
import net.zodac.tracker.framework.ExitState;
import net.zodac.tracker.framework.TrackerCredential;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.config.Configuration;
import net.zodac.tracker.framework.gui.DisplayValidator;
import net.zodac.tracker.web.BroadcastAppender;
import net.zodac.tracker.web.WebServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main class, which launches the application.
 */
public final class ApplicationLauncher {

    private static final Logger LOGGER = LogManager.getLogger();

    private ApplicationLauncher() {

    }

    /**
     * Main method for the application. Installs the SSE log appender then starts the embedded web server.
     *
     * @see WebServer
     */
    // TODO: Investigate whether it's worth not logging out
    static void main() {
        BroadcastAppender.install();
        WebServer.start(ApplicationLauncher::runExecution);
    }

    static void runExecution() {
        try {
            Configuration.get();
        } catch (final ExceptionInInitializerError e) {
            LOGGER.debug("Invalid environment variable", e);
            LOGGER.error("Invalid environment variable: {}", e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
            return;
        } catch (final Exception e) {
            LOGGER.debug("Unexpected error starting application pre-requisites", e);
            LOGGER.error("Unexpected error starting application pre-requisites: {}", e.getMessage());
            return;
        }

        final Map<TrackerType, Set<TrackerCredential>> trackersByType = TrackerRetriever.getTrackers();

        if (!DisplayValidator.isValid(trackersByType)) {
            return;
        }

        try {
            final ExitState exitState = ScreenshotOrchestrator.start(trackersByType);
            LOGGER.info("Execution completed: {}", exitState);
        } catch (final Exception e) {
            LOGGER.debug("Error abruptly ended execution", e);
            LOGGER.error("Error abruptly ended execution: {}", e.getMessage());
        }
    }
}
