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
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Extension of the {@link TsSpecialEditionHandler} for the {@code BackUps} tracker.
 */
@TrackerHandler(name = "BackUps", url = "https://back-ups.me/")
public class BackUpsHandler extends TsSpecialEditionHandler {

    @Override
    public By loginPageSelector() {
        return XpathBuilder
            .from(div, withClass("memberArea"))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected By usernameFieldSelector() {
        return By.id("usernameloginphp");
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(input, withClass("inputPassword"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(div, withId("main"))
            .descendant(input, withType("submit"), atIndex(1))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link BackUpsHandler}, having any unread private messages means you are redirected to your message inbox. We'll check if this is the case,
     * and then navigate to the home page.
     */
    @Override
    protected void manualCheckAfterLoginClick(final String trackerName) {
        scriptExecutor.waitForPageToLoad(WAIT_FOR_LOGIN_PAGE_LOAD);

        final String currentTitle = driver.getTitle();
        // TODO: Find a way to not use the title text, and be generic to language
        if (currentTitle == null || !currentTitle.contains("Private Messages in Folder: Inbox")) {
            LOGGER.debug("\t- No unread private messages");
            return;
        }

        LOGGER.debug("\t\t- Tracker redirected to inbox due to unread private messages, manually navigating back to the home page");
        final By homePageSelector = XpathBuilder
            .from(div, withId("menu"))
            .descendant(a, atIndex(1))
            .build();
        final WebElement homePageLink = driver.findElement(homePageSelector);
        clickButton(homePageLink);
    }

    @Override
    protected By logoutButtonSelector() {
        // Click the user dropdown menu bar to make the logout button interactable
        final By logoutParentSelector = By.id("quickprofileview");
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        clickButton(logoutParent);

        return XpathBuilder
            .from(div, withClass("qactions"))
            .descendant(a, atIndex(2))
            .build();
    }
}
