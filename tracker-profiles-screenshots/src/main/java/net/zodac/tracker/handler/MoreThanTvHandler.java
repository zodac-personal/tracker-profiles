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
import java.util.List;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code MoreThanTV} tracker.
 */
// TODO: Luminence
@TrackerHandler(name = "MoreThanTV", url = "https://www.morethantv.me/")
public class MoreThanTvHandler extends AbstractTrackerHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public MoreThanTvHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    public By loginPageSelector() {
        return By.xpath("//div[@id='logo']/ul[1]/li[2]/a[1]");
    }

    @Override
    protected By usernameFieldSelector() {
        return By.xpath("//div[@id='username']//input[1]");
    }

    @Override
    protected By passwordFieldSelector() {
        return By.xpath("//div[@id='password']//input[1]");
    }

    @Override
    protected By loginButtonSelector() {
        return By.id("login_button");
    }

    @Override
    protected By postLoginSelector() {
        return By.id("userinfo_username");
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            By.xpath("//ul[contains(@class, 'stats')]/li[3]/a[1]") // Email
        );
    }

    @Override
    public boolean hasFixedHeader() {
        final WebElement headerElement = driver.findElement(By.cssSelector("#menu, .main-menu"));
        scriptExecutor.makeUnfixed(headerElement);
        return true;
    }

    @Override
    protected By logoutButtonSelector() {
        // Highlight the profile menu to make the logout button interactable
        final By logoutParentSelector = By.id("userinfo_username");
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        scriptExecutor.moveTo(logoutParent);

        return By.xpath("//li[@id='nav_logout']/a[1]");
    }
}
