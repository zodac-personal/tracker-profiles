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

import net.zodac.tracker.framework.annotation.TrackerHandler;
import org.openqa.selenium.By;

/**
 * Extension of the {@link GazelleHandler} for the {@code Redacted} tracker.
 */
@TrackerHandler(name = "Redacted", url = "https://redacted.sh/")
public class Redacted extends GazelleHandler {

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link Redacted}, we check for a linked {@code Last.fm} account.
     */
    @Override
    protected By profilePageElementSelector() {
        return profilePageContentSelectorWithLastFm();
    }
}
