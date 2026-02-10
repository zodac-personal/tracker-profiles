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
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code Hawke-Uno} tracker.
 */
@TrackerHandler(name = "Hawke-Uno", type = TrackerType.CLOUDFLARE_CHECK, url = "https://hawke.uno/")
public class HawkeUnoHandler extends AbstractTrackerHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public HawkeUnoHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    protected By cloudflareSelector() {
        return DEFAULT_CLOUDFLARE_SELECTOR;
    }

    @Override
    protected By postLoginSelector() {
        return By.id("hoe-header");
    }

    @Override
    protected By profilePageSelector() {
        LOGGER.trace("Waiting {} for the login pop-up to disappear", DEFAULT_WAIT_FOR_PAGE_LOAD);
        ScriptExecutor.explicitWait(DEFAULT_WAIT_FOR_PAGE_LOAD);
        openUserDropdownMenu();
        return XpathBuilder
            .from(ul, withClass("dropdown-menu"))
            .child(li, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            // Email
            XpathBuilder
                .from(table, withClass("user-info"))
                .child(tbody)
                .child(tr)
                .child(td, atIndex(2))
                .build()
        );
    }

    @Override
    protected By logoutButtonSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(ul, withClass("dropdown-menu"))
            .descendant(button, withType("submit"))
            .build();
    }

    private void openUserDropdownMenu() {
        // Click the user dropdown menu bar to make the profile/logout button interactable
        final By profileParentSelector = XpathBuilder
            .from(li, withClass("hoe-header-profile"))
            .child(a, withClass("dropdown-toggle"))
            .build();
        final WebElement profileParent = driver.findElement(profileParentSelector);
        clickButton(profileParent);
    }
}
