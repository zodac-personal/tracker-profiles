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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link TimingUtils}.
 */
class TimingUtilsTest {

    @ParameterizedTest
    @CsvSource({
        // Milliseconds only
        "0, 0ms",
        "1, 1ms",
        "999, 999ms",

        // Seconds + ms
        "1000, 1s:000ms",
        "1037, 1s:037ms",
        "59000, 59s:000ms",
        "59999, 59s:999ms",

        // Minutes + seconds + ms
        "60000, 1m:00s:000ms",
        "61015, 1m:01s:015ms",
        "3599999, 59m:59s:999ms",

        // Hours (milliseconds dropped in output)
        "3600000, 1h:00m:00s",
        "3661000, 1h:01m:01s",
        "7322000, 2h:02m:02s"
    })
    void shouldFormatNaturalTimeCorrectly(final long inputMillis, final String expected) {
        final long input = TimeUnit.MILLISECONDS.toNanos(inputMillis);
        final String actual = TimingUtils.toNaturalTime(input);

        assertThat(actual)
            .isEqualTo(expected);
    }
}
