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
import static net.zodac.tracker.framework.xpath.HtmlElement.p;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.handler.definition.DoesNotScrollDuringScreenshot;
import net.zodac.tracker.handler.definition.HasCloudflareCheck;
import net.zodac.tracker.handler.definition.HasFixedHeader;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code Torrenting} tracker.
 */
@TrackerHandler(name = "Torrenting", type = TrackerType.MANUAL, url = "https://www.torrenting.com/")
public class Torrenting extends AbstractTrackerHandler implements DoesNotScrollDuringScreenshot, HasCloudflareCheck, HasFixedHeader {

    @Override
    public By cloudflareSelector() {
        return XpathBuilder
            .from(div, withClass("cf-turnstile"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(input, withClass("button"), withType("submit"))
            .build();
    }

    @Override
    protected By profileLinkSelector() {
        return By.id("usrnm");
    }

    @Override
    protected By profilePageElementSelector() {
        return By.id("userDetailsTable");
    }

    @Override
    public void unfixHeaders(final RemoteWebDriver driver, final List<By> headerSelectors) {
        // Special case where the header is made fixed due to JS injection, not HTML/CSS.
        // To make the header unfixed, we inject some CSS to override the existing logic.
        final String script = """
                var style = document.createElement('style');
                style.innerHTML = `
                    #navBarOuter {
                        position: static !important;
                        top: auto !important;
                    }
                    #navBarSpace {
                        height: 0 !important;
                    }
                `;
                document.head.appendChild(style);
            """;
        LOGGER.trace("Using script: {}", script);
        driver.executeScript(script);
    }

    @Override
    public List<By> headerSelectors() {
        // Not needed, unfixHeaders() has been overridden and doesn't use the selectors
        return List.of();
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(div, withId("userInfoDiv"))
            .child(p, atIndex(16))
            .child(a, atIndex(1))
            .build();
    }
}
