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
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.handler.definition.HasCloudflareCheck;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code YggTorrent} tracker.
 */
@TrackerHandler(name = "YggTorrent", type = TrackerType.CLOUDFLARE_CHECK, url = "https://www.yggtorrent.org/")
public class YggTorrent extends AbstractTrackerHandler implements HasCloudflareCheck {

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(input, withName("id"), withType("text"))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(input, withName("pass"), withType("password"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(form, withId("login-form"))
            .child(button, withType("submit"))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link YggTorrent}, after login there may be a pop-up offering a promotional sale on Turbo, which blocks the screen. We check for
     * the existence of the close button for these pop-ups, and click them to close if required before proceeding.
     *
     * @return the post-login {@link By} selector
     */
    @Override
    protected By postLoginSelector() {
        LOGGER.trace("Searching for promotion pop-up to close");
        final WebElement turboPromotionCloseButton = driver.findElement(By.id("turboPromoClose"));
        try {
            clickButton(turboPromotionCloseButton);
            LOGGER.debug("\t\t- Closing promotion for turbo before proceeding");
        } catch (final ElementNotInteractableException e) {
            LOGGER.trace("Promotion pop-up not interactable, nothing to close", e);
        }

        return By.id("panel-btn");
    }

    @Override
    protected By profilePageSelector() {
        // Click the username to open the user panel
        final WebElement profileParent = driver.findElement(By.id("panel-btn"));
        clickButton(profileParent);

        return XpathBuilder
            .from(div, withId("top_panel"))
            .child(div, atIndex(1))
            .child(div, atIndex(1))
            .child(ul, atIndex(3))
            .child(li, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            XpathBuilder
                .from(input, withClass("input-table"), withName("email"))
                .build()
        );
    }

    @Override
    protected Collection<By> passkeyElements() {
        return List.of(
            By.id("profile_passkey")
        );
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(div, withId("top_panel"))
            .child(div, atIndex(2))
            .child(ul, atIndex(1))
            .child(li, atIndex(7))
            .child(a, atIndex(1))
            .build();
    }
}
