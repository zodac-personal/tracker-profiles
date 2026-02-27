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
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withAttribute;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code AnimeTorrents} tracker.
 */
@TrackerHandler(name = "AnimeTorrents", type = TrackerType.CLOUDFLARE_CHECK, url = "https://animetorrents.me/")
public class AnimeTorrentsHandler extends AbstractTrackerHandler {

    @Override
    public By loginPageSelector() {
        return XpathBuilder
            .from(a, withAttribute("title", "Login to AnimeTorrents"))
            .build();
    }

    @Override
    protected By cloudflareSelector() {
        return DEFAULT_CLOUDFLARE_SELECTOR;
    }

    @Override
    protected By usernameFieldSelector() {
        return By.id("login-element-2");
    }

    @Override
    protected By passwordFieldSelector() {
        return By.id("login-element-3");
    }

    @Override
    protected By loginButtonSelector() {
        return By.id("login-element-6");
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(div, withId("UserPanel"))
            .child(ul, atIndex(1))
            .child(li, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            XpathBuilder
                .from(table, withClass("dataTable"))
                .child(tbody)
                .child(tr)
                .child(td, atIndex(2))
                .build()
        );
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(ul, withId("MmOtherLinks"))
            .child(li, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }
}
