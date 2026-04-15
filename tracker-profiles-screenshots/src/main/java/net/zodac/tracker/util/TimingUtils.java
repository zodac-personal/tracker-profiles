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

package net.zodac.tracker.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for time-related functions.
 */
public final class TimingUtils {

    private TimingUtils() {

    }

    /**
     * Converts the provided {@code elapsedTimeNanoseconds} into human-readable natural time.
     *
     * <p>
     * Durations are converted into a human-readable "natural time" format with millisecond precision. The output format adapts based on the magnitude
     * of the duration:
     *
     * <ul>
     *     <li>{@code 123ms}</li>
     *     <li>{@code 4s:037ms}</li>
     *     <li>{@code 2m:04s}</li>
     *     <li>{@code 1h:02m}</li>
     * </ul>
     *
     * <p>
     * Note that milliseconds are dropped if the time is greater than 1 minute. And seconds are dropped if the time is greater than 1 hour.
     *
     * @param elapsedTimeNanoseconds the elapsed time in nanoseconds
     * @return formatted duration {@link String}
     */
    public static String toNaturalTime(final long elapsedTimeNanoseconds) {
        final long totalMillis = TimeUnit.NANOSECONDS.toMillis(elapsedTimeNanoseconds);

        if (totalMillis < Duration.ofSeconds(1L).toMillis()) {
            return "%dms".formatted(totalMillis);
        }

        final long totalSeconds = totalMillis / 1000;
        final long millisPart = totalMillis % 1000;
        if (totalSeconds < Duration.ofMinutes(1L).toSeconds()) {
            return "%ds:%03dms".formatted(totalSeconds, millisPart);
        }

        final long minutes = totalSeconds / 60;
        final long secondsPart = totalSeconds % 60;
        if (totalSeconds < Duration.ofHours(1L).toSeconds()) {
            return "%dm:%02ds".formatted(minutes, secondsPart);
        }

        final long hours = totalSeconds / 3600;
        final long minutesPart = (totalSeconds % 3600) / 60;
        return "%dh:%02dm".formatted(hours, minutesPart);
    }
}
