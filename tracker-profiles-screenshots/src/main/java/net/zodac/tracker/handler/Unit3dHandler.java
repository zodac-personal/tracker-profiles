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
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.annotation.CommonTrackerHandler;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.NamedHtmlElement;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Common implementation of {@link AbstractTrackerHandler} for {@code UNIT3D}-based trackers.
 */
@CommonTrackerHandler("UNIT3D")
@TrackerHandler(name = "Aither", url = "https://aither.cc/")
@TrackerHandler(name = "F1Carreras", url = "https://f1carreras.xyz/")
@TrackerHandler(name = "HDUnited", url = "https://hd-united.vn/")
@TrackerHandler(name = "ItaTorrents", url = "https://itatorrents.xyz/")
@TrackerHandler(name = "OnlyEncodes", url = "https://onlyencodes.cc/")
@TrackerHandler(name = "PolishTorrent", url = "https://polishtorrent.top/")
@TrackerHandler(name = "Unwalled", url = "https://unwalled.cc/")
public class Unit3dHandler extends AbstractTrackerHandler {

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(button, withClass("auth-form__primary-button"))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        return XpathBuilder
            .from(ul, withClass("top-nav__ratio-bar"))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link Unit3dHandler}-based trackers, there is a cookie banner on first log-in.
     *
     * @return {@code true} once the banner is cleared
     */
    @Override
    public boolean canBannerBeCleared() {
        final By cookieSelector = XpathBuilder
            .from(button, withClass("cookie-consent__agree"))
            .build();
        final WebElement cookieButton = driver.findElement(cookieSelector);
        clickButton(cookieButton);

        // Move the mouse, or else a dropdown menu is highlighted and covers some of the page
        browserInteractionHelper.moveToOrigin();
        return true;
    }

    @Override
    protected By profilePageSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withClass("top-nav__right"))
            .descendant(li, withClass("top-nav__dropdown"))
            .child(ul, atIndex(1))
            .child(li, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            XpathBuilder
                .from(div, withClass("key-value__group"))
                .child(NamedHtmlElement.of("dd"))
                .build()
        );
    }

    @Override
    protected Collection<By> ipAddressElements() {
        return List.of(
            XpathBuilder
                .from(table, withClass("data-table"))
                .child(tbody)
                .child(tr)
                .child(td, atIndex(2))
                .build()
        );
    }

    @Override
    public boolean hasFixedHeader() {
        final WebElement headerElement = driver.findElement(By.tagName("header"));
        browserInteractionHelper.makeUnfixed(headerElement);
        return true;
    }

    @Override
    protected By logoutButtonSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withClass("top-nav__right"))
            .descendant(li, withClass("top-nav__dropdown"))
            .descendant(form, atIndex(1))
            .descendant(button, atIndex(1))
            .build();
    }

    /**
     * Opens the user's dropdown menu to expose links to the user profile and the logout button.
     */
    protected void openUserDropdownMenu() {
        // Highlight the nav bar to make the profile/logout button interactable
        final By logoutParentSelector = XpathBuilder
            .from(div, withClass("top-nav__right"))
            .descendant(li, withClass("top-nav__dropdown"))
            .build();
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        browserInteractionHelper.moveTo(logoutParent);
    }
}
