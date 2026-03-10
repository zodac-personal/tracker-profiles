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

package net.zodac.tracker.handler;

import java.time.Duration;
import net.zodac.tracker.framework.annotation.TrackerHandler;

/**
 * Extension of the {@link Unit3dHandler} for the {@code Concertos} tracker.
 */
@TrackerHandler(name = "Concertos", url = "https://concertos.live/")
public class Concertos extends Unit3dHandler {

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link Concertos}, unlike most other {@code UNIT3D}-based trackers, there is no cookie banner.
     *
     * @return {@code false} as there is no banner to be cleared
     */
    @Override
    public boolean canBannerBeCleared() {
        return false;
    }

    @Override
    protected Duration waitForPageLoadDuration() {
        return Duration.ofMinutes(2L);
    }
}
