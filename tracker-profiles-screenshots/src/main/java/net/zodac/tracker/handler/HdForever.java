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
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.NamedHtmlElement;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code HD-Forever} tracker.
 */
@TrackerHandler(name = "HD-Forever", url = "https://hdf.world/")
public class HdForever extends AbstractTrackerHandler {

    @Override
    protected By loginPageSelector() {
        return XpathBuilder
            .from(div, withId("public-nav"))
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
    protected By profilePageSelector() {
        return XpathBuilder
            .from(div, withId("userinfo_left"))
            .descendant(span, withClass("class_2"))
            .build();
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            // Sidebar email address
            XpathBuilder
                .from(ul, withClass("stats"))
                .child(li, atIndex(3))
                .child(a, atIndex(1))
                .build()
        );
    }

    @Override
    protected Collection<By> ipAddressElements() {
        return List.of(
            // Last connected IP address
            XpathBuilder
                .from(div, withId("footer"))
                .child(NamedHtmlElement.of("p"), atIndex(1))
                .child(a, atIndex(1))
                .child(span, atIndex(3))
                .build()
        );
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(div, withClass("user-actions"))
            .child(a, atIndex(2))
            .build();
    }
}
