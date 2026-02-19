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

import static net.zodac.tracker.framework.xpath.HtmlElement.div;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import net.zodac.tracker.framework.annotation.CommonTrackerHandler;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Common implementation of {@link GazelleHandler} for {@code Luminance}-based trackers.
 */
@CommonTrackerHandler("Luminance")
@TrackerHandler(name = "PixelCove", url = "https://www.pixelcove.me/")
@TrackerHandler(name = "PornBay", adult = true, url = "https://pornbay.org/")
public class LuminanceHandler extends GazelleHandler {

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(div, withId("username"))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(div, withId("password"))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return By.id("login_button");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link LuminanceHandler}, we also need to redact a passkey {@link WebElement}. We find the element defining the user's passkey in the
     * stats element on the profile page.
     *
     * @see AbstractTrackerHandler#redactElements()
     * @see net.zodac.tracker.redaction.Redactor#redactPasskey(WebElement)
     */
    @Override
    public int redactElements() {
        final WebElement passkeyElement = driver.findElement(passkeyElementSelector());
        redactor.redactPasskey(passkeyElement, "Passkey: ");

        return 1 + super.redactElements();
    }

    /**
     * Utility function that calls {@link #redactElements()} from {@link AbstractTrackerHandler}. This will allow any implementations of this class to
     * use the original function.
     *
     * @return the number of {@link WebElement}s where the text has been redacted
     */
    protected int originalRedactElements() {
        return super.redactElements();
    }

    /**
     * Defines the {@link By} selector for the user's passkey on the user profile page.
     *
     * @return the {@link By} selector for the user's passkey
     */
    protected By passkeyElementSelector() {
        return XpathBuilder
            .from(div, withClass("sidebar"))
            .child(div, atIndex(8))
            .child(ul, atIndex(1))
            .child(li, atIndex(4))
            .build();
    }
}
