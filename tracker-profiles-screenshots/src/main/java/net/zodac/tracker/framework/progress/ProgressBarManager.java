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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
 *
 * <p>
 * The Clique bar is driven by fine-grained ticks (one per workflow step: login, profile page, each screenshot, logout) so the fill and
 * percentage update smoothly. A separate tracker counter ({@code X/Y}) is appended to the bar content by {@link #getProgressBarContent()},
 * incremented once per completed tracker via {@link #tickTracker(String)}.
 */
public final class ProgressBarManager {

    private static final ApplicationConfiguration CONFIG = Configuration.get();
    private static final Logger LOGGER = LogManager.getLogger();

    private final Lock progressBarLock = new ReentrantLock();
    private final AtomicInteger completedTrackers = new AtomicInteger(0);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private @Nullable ProgressBar progressBar;
    private int trackerTotal;

    private ProgressBarManager() {

    }

    /**
     * Creates an instance of {@link ProgressBarManager}.
     *
     * @return the {@link ProgressBarManager}
     */
    public static ProgressBarManager create() {
        return new ProgressBarManager();
    }

    /**
     * Creates the progress bar and renders the initial (0%) state. Uses {@link ApplicationConfiguration} to configure the bar.
     *
     * <p>
     * The Clique bar is initialised with {@code ticksTotal} so the fill and percentage reflect fine-grained workflow progress. The tracker
     * counter displayed by {@link #getProgressBarContent()} uses {@code trackerTotal}, so users see {@code X/Y trackers} rather than raw
     * tick numbers.
     *
     * @param trackerTotal the total number of trackers to screenshot (used for the {@code X/Y} counter suffix)
     * @param ticksTotal   the total number of fine-grained ticks across all trackers (used for bar fill and percentage)
     */
    public void start(final int trackerTotal, final int ticksTotal) {
        this.trackerTotal = trackerTotal;
        this.completedTrackers.set(0);

        if (CONFIG.progressBarEnabled()) {
            final ProgressBarConfiguration progressBarConfiguration = ProgressBarConfiguration.builder()
                .length(CONFIG.progressBarLength())
                .complete(CONFIG.progressBarCompleteCharacter())
                .incomplete(CONFIG.progressBarIncompleteCharacter())
                .format(CONFIG.progressBarFormat())
                .build();
            LOGGER.trace("Starting progress bar with configuration: {}", progressBarConfiguration);
            progressBar = Clique.progressBar(ticksTotal, progressBarConfiguration);
        } else {
            LOGGER.debug("Not starting progress bar");
        }
    }

    /**
     * Returns {@code true} if the progress bar has been initialised, has not been {@link #stop() stopped}, and has not
     * {@link ProgressBar#isDone() completed}.
     *
     * @return {@code true} if the progress bar is active
     */
    public boolean isActive() {
        return !stopped.get() && progressBar != null && !progressBar.isDone();
    }

    /**
     * Returns {@code true} if the progress bar has reached its total (i.e. all ticks have been issued and Clique has emitted its completion
     * newline, leaving the cursor on the line below the bar).
     *
     * @return {@code true} if the progress bar is done
     */
    public boolean isDone() {
        return progressBar != null && progressBar.isDone();
    }

    /**
     * Marks the progress bar as stopped so that {@link #isActive()} returns {@code false} from this point on. Call this when all tracker work
     * is finished (e.g. from {@link net.zodac.tracker.framework.progress.ProgressBarPrintStream#close()}) to prevent the bar from being
     * re-rendered by any subsequent log output.
     */
    public void stop() {
        LOGGER.trace("Stopping progress bar");
        stopped.set(true);
    }

    /**
     * Advances the bar by one tick during tracker execution.
     *
     * @param trackerStep the {@link TrackerStep} being completed
     */
    public void tick(final TrackerStep trackerStep) {
        if (progressBar != null) {
            LOGGER.trace("Ticking progress bar for tracker step: {}", trackerStep.formattedName());
            progressBarLock.lock();
            try {
                progressBar.tick();
            } finally {
                progressBarLock.unlock();
            }
        }
    }

    /**
     * Increments the completed-tracker counter shown in the {@code X/Y} suffix of {@link #getProgressBarContent()}. Call once per tracker,
     * after all fine-grained ticks for that tracker have been issued.
     *
     * @param trackerName the name of the tracker
     */
    public void tickTracker(final String trackerName) {
        LOGGER.trace("Updating progress for tracker: {}", trackerName);
        completedTrackers.incrementAndGet();
    }

    /**
     * Returns the full bar string to render: the Clique-rendered bar followed by the tracker counter ({@code X/Y}) as a suffix.
     *
     * @return the rendered bar string, or an empty string if the bar has not been started
     */
    public String getProgressBarContent() {
        if (progressBar == null) {
            return "";
        }

        progressBarLock.lock();
        try {
            final int trackerTotalWidth = String.valueOf(trackerTotal).length();
            final String trackerCounter = ("%" + trackerTotalWidth + "d/%d").formatted(completedTrackers.get(), trackerTotal);
            return "%s | %s trackers".formatted(progressBar.get(), trackerCounter);
        } finally {
            progressBarLock.unlock();
        }
    }

    /**
     * Returns the lock used to coordinate progress bar rendering.
     *
     * <p>
     * {@link ProgressBarPrintStream} acquires this lock around each console write so that clearing the bar, emitting the log line, and
     * re-rendering the bar are atomic with respect to concurrent {@link #tick(TrackerStep)} calls from other threads.
     * Because {@link java.util.concurrent.locks.ReentrantLock} permits re-entry, holding this lock while calling
     * {@link #getProgressBarContent()} (which also acquires it) is safe.
     *
     * @return the progress bar {@link Lock}
     */
    public Lock getProgressBarLock() {
        return progressBarLock;
    }
}
