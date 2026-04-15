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

package net.zodac.tracker.framework.progress;

import io.github.kusoroadeolu.clique.Clique;
import io.github.kusoroadeolu.clique.components.ProgressBar;
import io.github.kusoroadeolu.clique.configuration.ProgressBarConfiguration;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Manages the {@link ProgressBar} rendered at the bottom of the console output during tracker execution.
 *
 * <p>
 * The bar is kept at the bottom of the console by {@link ProgressBarPrintStream}, which clears and re-renders it around every log line.
 */
public final class ProgressBarManager {

    private static final ApplicationConfiguration CONFIG = Configuration.get();
    private static final Logger LOGGER = LogManager.getLogger();

    private @Nullable ProgressBar progressBar;

    /**
     * Creates the progress bar for the given total tracker count and renders the initial (0%) state. Uses {@link ApplicationConfiguration} to
     * configure the progress bar.
     *
     * @param total the total number of trackers to screenshot
     */
    public void start(final int total) {
        if (CONFIG.progressBarEnabled()) {
            final ProgressBarConfiguration progressBarConfiguration = ProgressBarConfiguration.builder()
                .length(CONFIG.progressBarLength())
                .complete(CONFIG.progressBarCompleteCharacter())
                .incomplete(CONFIG.progressBarIncompleteCharacter())
                .format(CONFIG.progressBarFormat())
                .build();
            LOGGER.trace("Starting progress bar with configuration: {}", progressBarConfiguration);
            progressBar = Clique.progressBar(total, progressBarConfiguration);
        } else {
            LOGGER.debug("Not starting progress bar");
        }
    }

    /**
     * Returns {@code true} if the progress bar has been initialised and has not {@link ProgressBar#isDone()}.
     *
     * @return {@code true} if the progress bar is active
     */
    public boolean isActive() {
        return progressBar != null && !progressBar.isDone();
    }

    /**
     * Advances the progress bar by one tick.
     */
    public void tick() {
        if (progressBar != null) {
            progressBar.tick();
        }
    }

    public String getProgressBarContent() {
        return progressBar == null ? "" : progressBar.get();
    }
}
