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
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.handler.definition.HasFixedHeader;
import net.zodac.tracker.handler.definition.HasFixedSidebar;
import net.zodac.tracker.handler.definition.HasJumpButtons;
import net.zodac.tracker.redaction.RedactionBuffer;
import net.zodac.tracker.util.BrowserInteractionHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code DanishBytes} tracker.
 */
@TrackerHandler(name = "DanishBytes", url = "https://danishbytes.club/")
public class DanishBytes extends AbstractTrackerHandler implements HasFixedHeader, HasFixedSidebar, HasJumpButtons {

    @Override
    protected By usernameFieldSelector() {
        BrowserInteractionHelper.explicitWait(Duration.ofSeconds(1L), "web page to complete translation");
        return By.id("private_username");
    }

    @Override
    protected By passwordFieldSelector() {
        // The password field doesn't load until the username is entered and the 'Login' button is clicked
        final WebElement usernameLoginButton = driver.findElement(Objects.requireNonNull(super.loginButtonSelector()));
        clickButton(usernameLoginButton);
        browserInteractionHelper.waitForElementToBeInteractable(super.passwordFieldSelector(), pageLoadDuration());

        return super.passwordFieldSelector();
    }

    @Override
    protected By postLoginSelector() {
        return XpathBuilder
            .from(li, withClass("hoe-header-profile"))
            .child(a, withClass("dropdown-toggle"))
            .build();
    }

    @Override
    protected By profilePageSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(li, withClass("hoe-header-profile"))
            .child(ul, atIndex(1))
            .child(li, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected By profilePageContentSelector() {
        throw new UnsupportedOperationException("Site has moved to NordicBytes");
    }

    @Override
    public By headerSelector() {
        return XpathBuilder
            .from(div, withClass("hoe-right-header"))
            .build();
    }

    @Override
    public List<By> jumpButtonSelectors() {
        return List.of(
            By.id("back-to-top"),
            By.id("back-to-down")
        );
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            XpathBuilder
                .from(table)
                .child(tbody)
                .child(tr)
                .child(td, atIndex(2))
                .build()
        );
    }

    /**
     * The redaction doesn't cover the full {@code <li>} element for some reason, so we extend it to the left.
     *
     * @return the {@link RedactionBuffer} for IP address redaction
     */
    @Override
    protected RedactionBuffer emailElementBuffer() {
        return RedactionBuffer.withLeftOffset(13);
    }

    @Override
    protected By logoutButtonSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(li, withClass("hoe-header-profile"))
            .child(ul, atIndex(1))
            .child(li, atIndex(8))
            .child(a, atIndex(1))
            .build();
    }

    private void openUserDropdownMenu() {
        LOGGER.debug("\t\t- Waiting for login/rules pop-up to disappear");
        final By loginPopupSelector = By.id("swal2-title");
        browserInteractionHelper.waitForElementToDisappear(loginPopupSelector, pageLoadDuration());

        LOGGER.debug("\t\t- Clicking user dropdown menu to make profile/logout button interactable");
        final By profileParentSelector = XpathBuilder
            .from(li, withClass("hoe-header-profile"))
            .child(a, withClass("dropdown-toggle"))
            .build();

        final WebElement profileParent = driver.findElement(profileParentSelector);
        clickButton(profileParent);
    }
}
