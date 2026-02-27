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
import static net.zodac.tracker.framework.xpath.HtmlElement.img;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import java.time.Duration;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.BrowserInteractionHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code BeyondHD} tracker.
 */
@TrackerHandler(name = "BeyondHD", type = TrackerType.MANUAL, url = "https://beyond-hd.me/")
public class BeyondHdHandler extends AbstractTrackerHandler {

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link BeyondHdHandler}, prior to clicking the login button with a successful username/password there is another field where a Captcha
     * needs to be entered.
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
            .from(input, withId("captcha"))
            .parent(div)
            .build();
        final WebElement captchaElement = driver.findElement(captchaSelector);
        browserInteractionHelper.highlightElement(captchaElement);
        DisplayUtils.userInputConfirmation(trackerName, "Solve the captcha");
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(img, withClass("beta-image-avatar"))
            .parent(a)
            .build();
    }

    @Override
    public boolean hasSensitiveInformation() {
        return false;
    }

    @Override
    public boolean hasFixedHeader() {
        final WebElement headerElement = driver.findElement(By.id("stickyBar"));
        browserInteractionHelper.makeUnfixed(headerElement);
        return true;
    }

    @Override
    protected By logoutButtonSelector() {
        // Highlight the nav bar to make the logout button interactable
        final By logoutParentSelector = XpathBuilder
            .from(div, withClass("dropmenu"))
            .build();
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        browserInteractionHelper.moveTo(logoutParent);
        BrowserInteractionHelper.explicitWait(Duration.ofSeconds(1L), "dropdown menu for logout button to appear");

        return XpathBuilder
            .from(form, withId("logout-form1"))
            .parent(div)
            .child(a, atIndex(1))
            .build();
    }
}
