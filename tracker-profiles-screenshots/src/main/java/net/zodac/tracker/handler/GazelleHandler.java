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

import static net.zodac.tracker.framework.xpath.HtmlElement.a;
import static net.zodac.tracker.framework.xpath.HtmlElement.div;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.annotation.CommonTrackerHandler;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Common implementation of {@link AbstractTrackerHandler} for {@code Gazelle}-based trackers.
 */
@CommonTrackerHandler("Gazelle")
@TrackerHandler(name = "DICMusic", url = "https://dicmusic.com/")
@TrackerHandler(name = "Redacted", url = "https://redacted.sh/")
@TrackerHandler(name = "SecretCinema", url = "https://secret-cinema.pw/")
@TrackerHandler(name = "UHDBits", url = "https://uhdbits.org/")
public class GazelleHandler extends AbstractTrackerHandler {

    @Nullable
    @Override
    protected By loginPageSelector() {
        return XpathBuilder
            .from(div, withId("logo"))
            .child(ul, atIndex(1))
            .child(li, atIndex(2))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(input, withName("login"), withType("submit"))
            .build();
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            XpathBuilder
                .from(ul, withClass("stats"))
                .child(li)
                .child(a)
                .build()
        );
    }

    @Override
    protected Collection<By> ipAddressElements() {
        return List.of(
            // Last connected IP address
            XpathBuilder
                .from(div, withId("footer"))
                .descendant(a)
                .build(),
            // IP address in profile sidebar
            By.id("statuscont0")
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link GazelleHandler}-based trackers, sometimes the logout button is not visible until the link to the user's profile is hovered over.
     * This is not required for all trackers, but since it is a simple mouse move, we can execute it for all trackers and then return the appropriate
     * {@link By} selector.
     *
     * @return the {@link By} selector for the logout button
     */
    @Override
    protected By logoutButtonSelector() {
        // Highlight the profile menu to make the logout button interactable
        final By logoutParentSelector = XpathBuilder
            .from(a, withClass("username"))
            .build();
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        scriptExecutor.moveTo(logoutParent);

        return XpathBuilder
            .from(li, withId("nav_logout"))
            .child(a, atIndex(1))
            .build();
    }
}
