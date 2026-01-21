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

import java.util.Collection;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code BroadcasThe.Net} tracker.
 */
@TrackerHandler(name = "BroadcasThe.Net", type = TrackerType.CLOUDFLARE_CHECK, url = "https://broadcasthe.net/")
public class BroadcasTheNetHandler extends AbstractTrackerHandler {

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
        return By.xpath("//div[@id='logo']/a/button[normalize-space()='Login']");
    }

    @Override
    protected By cloudflareSelector() {
        return DEFAULT_CLOUDFLARE_SELECTOR;
    }

    @Override
    protected By loginButtonSelector() {
        return By.xpath("//input[@type='submit' and @value='Log In!']");
    }

    @Override
    protected By postLoginSelector() {
        return By.id("userinfo_username");
    }

    @Override
    protected By profilePageSelector() {
        return By.xpath("//li[@id='nav_user']/a[1]");
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

        final By infoTabSelector = By.xpath("//div[@id='slider']/div/ul/li/a[normalize-space()='Info']");
        final WebElement infoTabLink = driver.findElement(infoTabSelector);
        clickButton(infoTabLink);

        // Move the cursor out of the way, to avoid highlighting a tooltip for a badge
        scriptExecutor.moveToOrigin();
    }

    @Override
    protected By logoutButtonSelector() {
        return By.xpath("//ul[@id='userinfo_username']/li/a[normalize-space()='Logout']");
    }
}
