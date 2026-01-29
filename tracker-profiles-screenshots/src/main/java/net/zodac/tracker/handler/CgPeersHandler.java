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
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code CGPeers} tracker.
 */
@TrackerHandler(name = "CGPeers", type = TrackerType.CLOUDFLARE_CHECK, url = {
    "https://cgpeers.to/",
    "https://cgpeers.com/"
})
public class CgPeersHandler extends AbstractTrackerHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public CgPeersHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    public By loginPageSelector() {
        return By.xpath("//ul[contains(@class, 'nav-list')]/li[1]/a[1]");
    }

    @Override
    protected By cloudflareSelector() {
        return By.xpath("//div[contains(@class, 'cf-turnstile')]/div[1]");
    }

    @Override
    protected By usernameFieldSelector() {
        return By.xpath("//div[@id='username']/input[1]");
    }

    @Override
    protected By passwordFieldSelector() {
        return By.xpath("//div[@id='password']/input[1]");
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
     * that was sent to your email. This must be entered within {@link DisplayUtils#INPUT_WAIT_DURATION}.
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
        LOGGER.info("\t\t >>> Waiting for user to enter the 2FA code and click the 'Verify Code' button, for {} seconds",
            DisplayUtils.INPUT_WAIT_DURATION.getSeconds());

        final WebElement selectionElement = driver.findElement(By.xpath("//input[contains(@class, 'form-input')][@name='code']"));
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
        // Click the user dropdown menu bar to make the profile button interactable
        final By profileParentSelector = By.id("userDropdownTrigger");
        final WebElement profileParent = driver.findElement(profileParentSelector);
        clickButton(profileParent);

        return By.xpath("//div[contains(@class, 'dropdown-quick-actions-grid')]/a[1]");
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            By.xpath("//ul[contains(@class, 'stats')]/li") // Email
        );
    }

    @Override
    protected By logoutButtonSelector() {
        // Click the user dropdown menu bar to make the logout button interactable
        final By profileParentSelector = By.id("userDropdownTrigger");
        final WebElement profileParent = driver.findElement(profileParentSelector);
        clickButton(profileParent);

        return By.xpath("//div[contains(@class, 'dropdown-quick-actions-grid')]/a[6]");
    }
}
