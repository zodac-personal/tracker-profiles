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
import static net.zodac.tracker.framework.xpath.HtmlElement.form;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;

import java.time.Duration;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code Speed.CD} tracker.
 */
@TrackerHandler(name = "Speed.CD", type = TrackerType.CLOUDFLARE_CHECK, url = "https://speed.cd/")
public class SpeedCdHandler extends AbstractTrackerHandler {

    @Override
    protected By cloudflareSelector() {
        return DEFAULT_CLOUDFLARE_SELECTOR;
    }

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(input, withName("username"))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        // The password field doesn't load until the username is entered and the 'Next' button is clicked
        final By usernameNextButtonSelector = XpathBuilder
            .from(form, withClass("login"), atIndex(1))
            .child(div, atIndex(2))
            .child(input, atIndex(1))
            .build();
        final WebElement usernameNextButton = driver.findElement(usernameNextButtonSelector);
        clickButton(usernameNextButton);
        ScriptExecutor.explicitWait(Duration.ofSeconds(2L));

        return XpathBuilder
            .from(input, withName("pwd"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(form, withClass("login"), atIndex(2))
            .child(div, atIndex(2))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        return By.id("S_mailBtn");
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(div, withClass("tSta"))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(input, withClass("logOut"))
            .build();
    }
}
