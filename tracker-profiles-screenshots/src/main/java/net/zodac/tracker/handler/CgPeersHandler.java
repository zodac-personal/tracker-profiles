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
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code CGPeers} tracker.
 */
@TrackerHandler(name = "CGPeers", type = TrackerType.CLOUDFLARE_CHECK, url = {
    "https://cgpeers.to/",
    "https://cgpeers.com/"
})
public class CgPeersHandler extends AbstractTrackerHandler {

    @Override
    public By loginPageSelector() {
        return XpathBuilder
            .from(ul, withClass("nav-list"))
            .child(li, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected By cloudflareSelector() {
        return XpathBuilder
            .from(div, withClass("cf-turnstile"))
            .child(div, atIndex(1))
            .build();
    }

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(div, withId("username"))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(div, withId("password"))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return By.id("login_button");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link CgPeersHandler}, after clicking the login button with a successful username/password, an 2FA screen appears requesting a code
     * that was sent to your email.
     *
     * <p>
     * Manual user interactions:
     * <ol>
     *     <li>Enter the emailed 2FA code</li>
     *     <li>Click the 'Verify Code' button</li>
     * </ol>
     */
    @Override
    protected void manualCheckAfterLoginClick(final String trackerName) {
        final String initialUrl = driver.getCurrentUrl();
        LOGGER.info("\t\t >>> Waiting for user to enter the 2FA code and click the 'Verify Code' button");

        final By selectionSelector = XpathBuilder
            .from(input, withClass("form-input"), withName("code"))
            .build();
        final WebElement selectionElement = driver.findElement(selectionSelector);
        scriptExecutor.highlightElement(selectionElement);
        DisplayUtils.userInputConfirmation(trackerName, "Enter the 2FA code and click the 'Verify Code' button");

        // If the user didn't click 'Verify Code', do it for them (it shares the same HTML ID as the login button from the previous page)
        final String nextUrl = driver.getCurrentUrl();
        if (nextUrl == null || nextUrl.equalsIgnoreCase(initialUrl)) {
            final By loginButtonSelector = loginButtonSelector();
            if (loginButtonSelector != null) {
                final WebElement loginButton = driver.findElement(loginButtonSelector);
                clickButton(loginButton);
            }
        }
    }

    @Override
    protected By postLoginSelector() {
        return By.id("userDropdownTrigger");
    }

    @Override
    protected By profilePageSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withClass("dropdown-quick-actions-grid"))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            XpathBuilder
                .from(ul, withClass("stats"))
                .child(li)
                .build()
        );
    }

    @Override
    protected Collection<By> ipAddressElements() {
        return List.of(
            XpathBuilder
                .from(table, withClass("cgp-table-main"))
                .child(tbody)
                .child(tr, atIndex(2))
                .child(td, atIndex(2))
                .child(a)
                .build()
        );
    }

    @Override
    protected By logoutButtonSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withClass("dropdown-quick-actions-grid"))
            .child(a, atIndex(6))
            .build();
    }

    private void openUserDropdownMenu() {
        // Click the user dropdown menu bar to make the profile/logout button interactable
        final By profileParentSelector = By.id("userDropdownTrigger");
        final WebElement profileParent = driver.findElement(profileParentSelector);
        clickButton(profileParent);
    }
}
