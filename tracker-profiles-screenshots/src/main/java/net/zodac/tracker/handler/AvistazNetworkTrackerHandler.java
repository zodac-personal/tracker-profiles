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
import static net.zodac.tracker.framework.xpath.HtmlElement.img;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.CommonTrackerHandler;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.framework.xpath.NamedHtmlElement;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code AvistaZ} Network of trackers.
 */
@CommonTrackerHandler("AvistaZNetwork")
@TrackerHandler(name = "AvistaZ", type = TrackerType.CLOUDFLARE_CHECK, url = "https://avistaz.to/")
@TrackerHandler(name = "CinemaZ", type = TrackerType.CLOUDFLARE_CHECK, url = "https://cinemaz.to/")
@TrackerHandler(name = "PrivateHD", type = TrackerType.CLOUDFLARE_CHECK, url = "https://privatehd.to/")
public class AvistazNetworkTrackerHandler extends AbstractTrackerHandler {

    @Override
    public By loginPageSelector() {
        return XpathBuilder
            .from(div, withId("navbar"))
            .child(ul, atIndex(2))
            .child(li, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected By cloudflareSelector() {
        return DEFAULT_CLOUDFLARE_SELECTOR;
    }

    @Override
    protected By usernameFieldSelector() {
        return By.id("email_username");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link AvistazNetworkTrackerHandler}, prior to clicking the login button with a successful username/password there is another field where a
     * Captcha needs to be entered.
     *
     * <p>
     * Manual user interaction:
     * <ol>
     *     <li>Enter correct captcha value</li>
     * </ol>
     */
    @Override
    protected void manualCheckBeforeLoginClick(final String trackerName) {
        LOGGER.info("\t\t >>> Waiting for user to enter captcha");

        final By captchaSelector = XpathBuilder
            .from(input, withName("captcha"))
            .parent(div)
            .child(div, atIndex(1))
            .child(img, atIndex(1))
            .build();
        final WebElement captchaElement = driver.findElement(captchaSelector);
        browserInteractionHelper.highlightElement(captchaElement);
        DisplayUtils.userInputConfirmation(trackerName, "Solve the captcha");
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(input, withType("submit"))
            .build();
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(span, withClass("badge-user"))
            .parent(a)
            .build();
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            XpathBuilder
                .from(NamedHtmlElement.of("section"), withId("content-area"))
                .child(div, atIndex(2))
                .child(table, atIndex(2))
                .child(tbody, atIndex(1))
                .child(tr, atIndex(3))
                .child(td, atIndex(2))
                .build()
        );
    }

    @Override
    protected Collection<By> ipAddressElements() {
        return List.of(
            XpathBuilder
                .from(NamedHtmlElement.of("section"), withId("content-area"))
                .child(div, atIndex(2))
                .child(table, atIndex(2))
                .child(tbody, atIndex(1))
                .child(tr, atIndex(4))
                .child(td, atIndex(2))
                .build()
        );
    }

    @Override
    public boolean hasFixedHeader() {
        final By headerSelector = XpathBuilder
            .from(NamedHtmlElement.of("nav"), withClass("navbar-fixed-top"))
            .build();
        final WebElement headerElement = driver.findElement(headerSelector);
        browserInteractionHelper.makeUnfixed(headerElement);
        return true;
    }

    @Override
    protected By logoutButtonSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withClass("navbar"))
            .child(ul, atIndex(2))
            .child(li, atIndex(3))
            .child(ul, atIndex(1))
            .child(li, atIndex(16))
            .build();
    }

    /**
     * Opens the user's dropdown menu to expose links to the user profile and the logout button.
     */
    protected void openUserDropdownMenu() {
        // Click the user dropdown menu bar to make the profile/logout button interactable
        final By logoutParentSelector = XpathBuilder
            .from(div, withClass("navbar"))
            .child(ul, atIndex(2))
            .child(li, atIndex(3))
            .build();
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        clickButton(logoutParent);
    }
}
