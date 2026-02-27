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

import static net.zodac.tracker.framework.xpath.HtmlElement.button;
import static net.zodac.tracker.framework.xpath.HtmlElement.div;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code Milkie} tracker.
 */
@TrackerHandler(name = "Milkie", url = "https://milkie.cc/")
public class MilkieHandler extends AbstractTrackerHandler {

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(input, withId("mat-input-0"))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(input, withId("mat-input-1"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(button, withType("submit"))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link MilkieHandler}, there is no user profile page. There is simply a pop-up with the user's stats. So we click the pop-up to display
     * stats but there is no new page loaded.
     */
    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(button, withClass("profile-button"))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link MilkieHandler}, because there is no user profile page we take a screenshot of the main tracker page. This is a large list of
     * torrents and there is no value to scrolling, so we just screenshot the page once the pop-up appears.
     */
    @Override
    public boolean scrollDuringScreenshot() {
        return false;
    }

    @Override
    protected By logoutButtonSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withClass("cdk-overlay-pane"))
            .child(div, atIndex(2))
            .child(div, atIndex(1))
            .child(div, atIndex(2))
            .child(span, atIndex(2))
            .build();
    }

    protected void openUserDropdownMenu() {
        reloadPage(); // Reload the page to have a consistent state with no user stats pop-up
        final By profilePageSelector = profilePageSelector();

        // TODO: Move this to browserInteractionHelper, and replace more explicit waits with element visibily waits
        final Wait<WebDriver> wait = new WebDriverWait(driver, waitForPageLoadDuration());
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(profilePageSelector));

        final WebElement logoutParent = driver.findElement(profilePageSelector);
        clickButton(logoutParent);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link MilkieHandler}, the {@link #usernameFieldSelector()} is not valid, since after a logout the ID of the {@link WebElement} is
     * different. However, the {@link #loginButtonSelector()} is still the same, so we can reuse that.
     */
    @Override
    protected By postLogoutElementSelector() {
        return loginButtonSelector();
    }
}
