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
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withAttribute;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.framework.xpath.NamedHtmlElement;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code DigitalCore.Club} tracker.
 */
@TrackerHandler(name = "DigitalCore.Club", type = TrackerType.MANUAL, url = {
    "https://digitalcore.club/",
    "https://prxy.digitalcore.club/"
})
public class DigitalCoreClubHandler extends AbstractTrackerHandler {

    @Override
    protected By usernameFieldSelector() {
        return By.id("inputUsername");
    }

    @Override
    protected By passwordFieldSelector() {
        return By.id("inputPassword");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link DigitalCoreClubHandler}, prior to clicking the login button with a successful username/password there is another field where a
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

        final WebElement captchaElement = driver.findElement(By.id("captcha"));
        scriptExecutor.highlightElement(captchaElement);
        DisplayUtils.userInputConfirmation(trackerName, "Solve the captcha");
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(button, withAttribute("translate", "LOGIN.TITLE"))
            .build();
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(NamedHtmlElement.of("user"), atIndex(1))
            .descendant(a, atIndex(1))
            .build();
    }

    @Override
    public boolean hasSensitiveInformation() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link DigitalCoreClubHandler}, the page loads but the table with user details is not visible on the initial load. So we wait for the user
     * details table to be visible before proceeding.
     */
    @Override
    protected void additionalActionOnProfilePage() {
        ScriptExecutor.explicitWait(waitForTransitionsDuration(), "user details to load");

        final By selector = XpathBuilder
            .from(div, withId("contentContainer"))
            .descendant(table, atIndex(1))
            .descendant(td, withAttribute("translate", "FRIENDS.LAST_SEEN"))
            .build();
        scriptExecutor.waitForElementToAppear(selector, waitForPageLoadDuration());
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(span, withAttribute("translate", "STATUS.LOG_OUT"))
            .build();
    }
}
