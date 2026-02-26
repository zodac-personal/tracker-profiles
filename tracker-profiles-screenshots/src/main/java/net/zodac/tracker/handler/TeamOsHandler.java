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
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withText;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code TeamOS} tracker.
 *
 * <p>
 * Note that the URL is set to the login page in order to bypass the site redirecting to an advertisement.
 */
@TrackerHandler(name = "TeamOS", url = "https://teamos.xyz/login")
public class TeamOsHandler extends AbstractTrackerHandler {

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(input, withName("login"), withType("text"))
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link TeamOsHandler}, the tracker can request you to read a thread after logging in. Since this page looks very similar to the normal
     * post-login page, we can't simply rely on {@link #postLoginSelector()}. Instead, we'll explicitly search for this request to read a thread. If
     * it exists, we'll open the page (forcing it into the current tab). We'll then continue with the rest of the flow.
     */
    @Override
    protected void manualCheckAfterLoginClick(final String trackerName) {
        scriptExecutor.stopPageLoad();

        final String title = driver.getTitle();
        // TODO: Test this again to be language agnostic
        if (title != null && title.contains("Oops! We ran into some problems.")) {
            LOGGER.debug("\t\t- Tracker admin requires updated thread to be viewed, clicking...");
            final By threadRedirectSelector = XpathBuilder
                .from(span, withText("You can click here to view the thread"))
                .parent(a)
                .build();
            final WebElement threadRedirect = driver.findElement(threadRedirectSelector);

            scriptExecutor.removeAttribute(threadRedirect, "target"); // Stop forcing the link to open in a new tab
            clickButton(threadRedirect);
            scriptExecutor.stopPageLoad();
        }
    }

    @Override
    protected By postLoginSelector() {
        return XpathBuilder
            .from(div, withClass("focus-wrap-user"))
            .build();
    }

    @Override
    protected By profilePageSelector() {
        navigateToUserPage();
        return XpathBuilder
            .from(div, withClass("p-body-sideNavContent"))
            .child(div, atIndex(1))
            .child(div, atIndex(1))
            .child(div, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    public boolean hasSensitiveInformation() {
        return false;
    }

    @Override
    protected By logoutButtonSelector() {
        navigateToUserPage();
        return XpathBuilder
            .from(div, withClass("p-body-sideNavContent"))
            .child(div, atIndex(2))
            .child(div, atIndex(1))
            .child(div, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    private void navigateToUserPage() {
        ScriptExecutor.explicitWait(waitForPageUpdateDuration(), "page to load before clicking navbar");
        // Click the nav bar to make the profile button interactable
        final By profileParentSelector = XpathBuilder
            .from(a, withClass("p-navgroup-link--user"))
            .build();
        final WebElement profileParent = driver.findElement(profileParentSelector);
        clickButton(profileParent);
        ScriptExecutor.explicitWait(waitForPageUpdateDuration(), "first navbar click");

        clickButton(profileParent);
        ScriptExecutor.explicitWait(waitForPageUpdateDuration(), "second navbar click");
    }

    // TODO: Have a before/after screenshot section, where this tracker's bespoke scrollbar can be explicitly hidden?

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link TeamOsHandler}, after logout we are redirected to the homepage, not the login page. While we actually ignore this page during login
     * (due to the fact we are redirected to an advertisement), we need to specify it now to confirm logout.
     */
    @Override
    protected By postLogoutElementSelector() {
        return XpathBuilder
            .from(a, withClass("p-navgroup-link--logIn"))
            .build();
    }

    @Override
    protected boolean installAdBlocker() {
        return true;
    }
}
