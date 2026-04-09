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
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code NordicHD} tracker.
 */
@TrackerHandler(name = "NordicHD", url = "https://nordichd.org/")
public class NordicHd extends AbstractTrackerHandler {

    @Override
    protected By usernameFieldSelector() {
        return By.id("loginbox_membername");
    }

    @Override
    protected By passwordFieldSelector() {
        return By.id("loginbox_password");
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(input, withClass("submit"), withType("submit"))
            .build();
    }

    @Override
    protected By profileLinkSelector() {
        return XpathBuilder
            .from(div, withClass("eLinks"))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected By profilePageElementSelector() {
        return By.id("memberCard");
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(div, withClass("eLinks"))
            .child(a, atIndex(2))
            .build();
    }
}
