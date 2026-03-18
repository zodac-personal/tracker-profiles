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

import java.util.List;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.handler.definition.HasJumpButtons;
import net.zodac.tracker.redaction.OverlayBuffer;
import org.openqa.selenium.By;

/**
 * Extension of the {@link GazelleHandler} for the {@code DICMusic} tracker.
 */
@TrackerHandler(name = "DICMusic", url = "https://dicmusic.com/")
public class DicMusic extends GazelleHandler implements HasJumpButtons {

    @Override
    public List<By> jumpButtonSelectors() {
        return List.of(
            By.id("back-to-top-btn")
        );
    }

    /**
     * The overlay doesn't cover the full {@code <li>} element for some reason, so we extend the overlay to the left.
     *
     * @return the {@link OverlayBuffer} for IP address redaction
     */
    @Override
    protected OverlayBuffer emailElementBuffer() {
        return OverlayBuffer.withLeftOffset(7);
    }
}
