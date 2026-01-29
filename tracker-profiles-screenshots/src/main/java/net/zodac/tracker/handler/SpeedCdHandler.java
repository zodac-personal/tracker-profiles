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
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code Speed.CD} tracker.
 */
@TrackerHandler(name = "Speed.CD", type = TrackerType.CLOUDFLARE_CHECK, url = "https://speed.cd/")
public class SpeedCdHandler extends AbstractTrackerHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public SpeedCdHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    protected By cloudflareSelector() {
        return DEFAULT_CLOUDFLARE_SELECTOR;
    }

    @Override
    protected By usernameFieldSelector() {
        return By.xpath("//input[@name='username']");
    }

    @Override
    protected By passwordFieldSelector() {
        // The password field doesn't load until the username is entered and the 'Next' button is clicked
        final By usernameNextButtonSelector = By.xpath("//form[contains(@class, 'login')][1]/div[2]/input[1]");
        final WebElement usernameNextButton = driver.findElement(usernameNextButtonSelector);
        clickButton(usernameNextButton);
        ScriptExecutor.explicitWait(Duration.ofSeconds(2L));

        return By.xpath("//input[@name='pwd']");
    }

    @Override
    protected By loginButtonSelector() {
        return By.xpath("//form[contains(@class, 'login')][2]/div[2]/input[1]");
    }

    @Override
    protected By postLoginSelector() {
        return By.id("S_mailBtn");
    }

    @Override
    protected By profilePageSelector() {
        return By.xpath("//div[contains(@class, 'tSta')]/a[1]");
    }

    @Override
    protected By logoutButtonSelector() {
        return By.xpath("//input[contains(@class, 'logOut')]");
    }

    @Override
    protected By postLogoutElementSelector() {
        return usernameFieldSelector();
    }
}
