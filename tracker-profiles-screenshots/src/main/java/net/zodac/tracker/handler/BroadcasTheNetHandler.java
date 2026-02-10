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
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Extension of the {@link GazelleHandler} for the {@code BroadcasThe.Net} tracker.
 */
@TrackerHandler(name = "BroadcasThe.Net", type = TrackerType.CLOUDFLARE_CHECK, url = "https://broadcasthe.net/")
public class BroadcasTheNetHandler extends GazelleHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public BroadcasTheNetHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    public By loginPageSelector() {
        return XpathBuilder
            .from(div, withId("logo"))
            .child(a, atIndex(2))
            .build();
    }

    @Override
    protected By cloudflareSelector() {
        return DEFAULT_CLOUDFLARE_SELECTOR;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link BroadcasTheNetHandler}, the initial profile page has no user stats. We need to click the 'Info' tab to expose this information for a
     * screenshot.
     */
    @Override
    protected void additionalActionOnProfilePage() {
        // Reload the page, to ensure the section closing works (JS may have been cancelled earlier)
        driver.navigate().refresh();
        scriptExecutor.waitForPageToLoad(DEFAULT_WAIT_FOR_PAGE_LOAD);

        final By infoTabSelector = XpathBuilder
            .from(div, withId("slider"))
            .child(div, atIndex(1))
            .child(ul, atIndex(1))
            .child(li, atIndex(2))
            .child(a, atIndex(1))
            .build();
        final WebElement infoTabLink = driver.findElement(infoTabSelector);
        clickButton(infoTabLink);

        // Move the cursor out of the way, to avoid highlighting a tooltip for a badge
        scriptExecutor.moveToOrigin();
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of();
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(ul, withId("userinfo_username"))
            .child(li, atIndex(3))
            .child(a, atIndex(1))
            .build();
    }
}
