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
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.util.BrowserInteractionHelper;
import org.openqa.selenium.By;

/**
 * Extension of the {@link Unit3dHandler} for the {@code Immortal-S} tracker.
 *
 * <p>
 * Note that the {@link TrackerType#MANUAL} is set due to the Cloudflare check. While it doesn't require any user interaction, it does not work in
 * headless mode so we need to enforce the browser launches with a UI.
 */
@TrackerHandler(name = "Immortal-S", type = TrackerType.MANUAL, url = "https://immortal-s.me/login/")
public class ImmortalS extends XenForoHandler {

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link ImmortalS}, there is a Cloudflare check on the login page. We need to wait for it to succeed before entering credentials and
     * attempting to log in. Since it is a Cloudflare element, we can't wait for it or evaluate its state, so we have an
     * {@link BrowserInteractionHelper#explicitWait(Duration, String)}.
     *
     * @return the username field {@link By} selector
     */
    @Override
    protected By usernameFieldSelector() {
        BrowserInteractionHelper.explicitWait(Duration.ofSeconds(2L), "Cloudflare check to pass");
        return super.usernameFieldSelector();
    }
}
