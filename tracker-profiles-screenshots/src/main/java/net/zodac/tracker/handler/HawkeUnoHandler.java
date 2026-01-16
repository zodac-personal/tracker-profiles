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

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
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
    protected boolean hasCloudflareCheck() {
        return true;
    }

    @Override
    protected By postLoginSelector() {
        return By.xpath("//div[contains(@class, 'ratio-bar')]");
    }

    @Override
    protected By profilePageSelector() {
        // After login, a popup blocks the dropdown menu. So we wait a couple of seconds
        ScriptExecutor.explicitWait(Duration.ofSeconds(2L));

        // Click the user dropdown menu bar to make the profile button interactable
        final By profileParentSelector = By.xpath("//ul[contains(@class, 'right-navbar')]/li[3]/a[1]");
        final WebElement profileParent = driver.findElement(profileParentSelector);
        clickButton(profileParent);

        return By.xpath("//ul[@class='right-navbar']//ul[@class='dropdown-menu']/li[1]/a[1]");
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            By.xpath("//table[contains(@class, 'user-info')]/tbody/tr[td[text()='E-mail']]/td[2]") // Email
        );
    }

    @Override
    protected By logoutButtonSelector() {
        // Click the user dropdown menu bar to make the logout button interactable
        final By profileParentSelector = By.xpath("//ul[contains(@class, 'right-navbar')]/li[3]/a[1]");
        final WebElement profileParent = driver.findElement(profileParentSelector);
        clickButton(profileParent);

        return By.xpath("//form[@role='form' and @method='POST']//button[@type='submit']");
    }
}
