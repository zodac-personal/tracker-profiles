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
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code PixelHD} tracker.
 */
@TrackerHandler(name = "PixelHD", url = "https://pixelhd.me/")
public class PixelHdHandler extends AbstractTrackerHandler {

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
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(form, withClass("fade_in"))
            .child(table, atIndex(1))
            .child(tbody, atIndex(1))
            .child(tr, atIndex(1))
            .child(td, atIndex(2))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(form, withClass("fade_in"))
            .child(table, atIndex(1))
            .child(tbody, atIndex(1))
            .child(tr, atIndex(2))
            .child(td, atIndex(2))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(form, withClass("fade_in"))
            .child(table, atIndex(1))
            .child(tbody, atIndex(1))
            .child(tr, atIndex(3))
            .child(td, atIndex(3))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By profilePageSelector() {
        return By.id("userNameMenu");
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            XpathBuilder
                .from(div, withClass("sidebar"))
                .child(div, atIndex(5))
                .child(ul, atIndex(1))
                .child(li, atIndex(3))
                .child(a, atIndex(1))
                .build()
        );
    }

    @Override
    protected Collection<By> passkeyElements() {
        return List.of(
            XpathBuilder
                .from(div, withClass("sidebar"))
                .child(div, atIndex(5))
                .child(ul, atIndex(1))
                .child(li, atIndex(4))
                .build()
        );
    }

    @Override
    protected By logoutButtonSelector() {
        return By.id("logoutlink");
    }
}
