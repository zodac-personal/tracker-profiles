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
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.time.Duration;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.NamedHtmlElement;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.BrowserInteractionHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code C411} tracker.
 */
@TrackerHandler(name = "C411", url = "https://c411.org/")
public class C411 extends AbstractTrackerHandler {

    @Override
    protected By usernameFieldSelector() {
        BrowserInteractionHelper.explicitWait(Duration.ofSeconds(1L), "web page to complete translation");
        return XpathBuilder
            .from(input, withName("username"), withType("text"))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(input, withName("password"), withType("password"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(button, withType("submit"))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        return By.id("reka-dropdown-menu-trigger-v-1-0-2");
    }

    @Override
    protected By profilePageSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withId("reka-dropdown-menu-content-v-1-0-4"))
            .child(div, atIndex(1))
            .child(div, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    public boolean hasFixedHeader() {
        final By headerSelector = XpathBuilder
            .from(div, withId("__nuxt"))
            .child(div, atIndex(2))
            .child(NamedHtmlElement.of("header"), atIndex(1))
            .build();
        final WebElement headerElement = driver.findElement(headerSelector);
        browserInteractionHelper.makeUnfixed(headerElement);
        return true;
    }

    @Override
    protected By logoutButtonSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withId("reka-dropdown-menu-content-v-1-0-4"))
            .child(div, atIndex(1))
            .child(div, atIndex(3))
            .child(button, atIndex(1))
            .build();
    }

    private void openUserDropdownMenu() {
        // Click the user dropdown menu bar to make the profile/logout button interactable
        final By profileParentSelector = By.id("reka-dropdown-menu-trigger-v-1-0-2");
        final WebElement profileParent = driver.findElement(profileParentSelector);
        clickButton(profileParent);
    }
}
