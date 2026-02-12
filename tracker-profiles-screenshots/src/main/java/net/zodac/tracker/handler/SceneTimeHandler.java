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
import static net.zodac.tracker.framework.xpath.HtmlElement.button;
import static net.zodac.tracker.framework.xpath.HtmlElement.div;
import static net.zodac.tracker.framework.xpath.HtmlElement.form;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code SceneTime} tracker.
 */
@TrackerHandler(name = "SceneTime", type = TrackerType.CLOUDFLARE_CHECK, url = "https://scenetime.com/")
public class SceneTimeHandler extends AbstractTrackerHandler {

    @Override
    protected By cloudflareSelector() {
        return DEFAULT_CLOUDFLARE_SELECTOR;
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(form, withId("login-form"))
            .child(div, atIndex(1))
            .child(button, atIndex(1))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        return By.id("Statusdiv");
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(div, withId("Statusdiv"))
            .child(div, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link SceneTimeHandler}, there is also a table entry with our passkey. We find the {@literal <}{@code tr}{@literal >} {@link WebElement}s
     * which has a {@literal <}{@code td}{@literal >} {@link WebElement} with the text value <b>Pass Key</b>. From this
     * {@literal <}{@code tr}{@literal >}, we find the child {@literal <}{@code td}{@literal >}, which needs its content redacted.
     *
     * @see AbstractTrackerHandler#redactElements()
     * @see net.zodac.tracker.redaction.Redactor#redactPasskey(WebElement)
     */
    @Override
    public int redactElements() {
        final By passkeySelector = XpathBuilder
            .from(table, withClass("main"))
            .descendant(table, atIndex(1))
            .child(tbody, atIndex(1))
            .child(tr, atIndex(4))
            .child(td, atIndex(2))
            .build();
        final WebElement passkeyValueElement = driver.findElement(passkeySelector);
        redactor.redactPasskey(passkeyValueElement);

        return 1 + super.redactElements();
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            // Email
            XpathBuilder
                .from(table)
                .child(tbody, atIndex(1))
                .child(tr)
                .child(td, atIndex(2))
                .build()
        );
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(div, withId("Statusdiv"))
            .child(div, atIndex(1))
            .child(a, atIndex(2))
            .build();
    }
}
