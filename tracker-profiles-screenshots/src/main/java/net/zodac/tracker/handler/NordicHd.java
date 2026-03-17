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
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withAttribute;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code NordicHD} tracker.
 */
@TrackerHandler(name = "NordicHD", url = "http://cinefilhd.eu/")
public class NordicHd extends AbstractTrackerHandler {

    @Override
    protected By usernameFieldSelector() {
        return By.id("nickname");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link NordicHd}, there are multiple buttons for login. While only one of them has the value 'LOGIN', all of them seem to work, so
     * for now we just take the first one.
     *
     * @return the login button {@link By} selector
     */
    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(form, withAttribute("method", "post"))
            .child(table, atIndex(1))
            .child(tbody, atIndex(1))
            .child(tr, atIndex(6))
            .child(td, atIndex(1))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(div, withId("statusbar"))
            .child(div, atIndex(1))
            .child(span, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected Collection<By> ipAddressElements() {
        return List.of(
            XpathBuilder
                .from(div, withId("general"))
                .child(table, atIndex(1))
                .child(tbody, atIndex(1))
                .child(tr, atIndex(4))
                .child(td, atIndex(2))
                .build()
        );
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(div, withId("gm_taps_right"))
            .child(ul, atIndex(1))
            .child(li, atIndex(11))
            .child(a, atIndex(1))
            .build();
    }
}
