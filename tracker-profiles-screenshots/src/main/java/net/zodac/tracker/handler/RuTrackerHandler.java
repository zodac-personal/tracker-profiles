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

import static net.zodac.tracker.framework.xpath.HtmlElement.img;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.time.Duration;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code RUTracker} tracker.
 */
@TrackerHandler(name = "RUTracker", url = {
    // Link direct to the tracker page
    "https://rutracker.org/forum/tracker.php",
    "https://rutracker.net/forum/tracker.php"
})
public class RuTrackerHandler extends AbstractTrackerHandler {

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(table, withClass("forumline"))
            .descendant(input, withName("login_username"), withType("text"))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(table, withClass("forumline"))
            .descendant(input, withName("login_password"), withType("password"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(table, withClass("forumline"))
            .descendant(input, withName("login"), withType("submit"))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        return By.id("logged-in-username");
    }

    @Override
    protected By profilePageSelector() {
        return By.id("logged-in-username");
    }

    @Override
    public boolean hasSensitiveInformation() {
        return false;
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(img, withClass("log-out-icon"))
            .build();
    }

    @Override
    protected By postLogoutElementSelector() {
        return By.id("quick-search-guest");
    }

    @Override
    protected Duration maximumClickResolutionDuration() {
        return Duration.ofMinutes(2L);
    }

    @Override
    protected Duration waitForPageLoadDuration() {
        return Duration.ofMinutes(1L);
    }

    @Override
    protected boolean installAdBlocker() {
        return true;
    }
}
