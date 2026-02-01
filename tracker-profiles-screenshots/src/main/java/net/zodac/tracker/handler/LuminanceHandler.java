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
import net.zodac.tracker.framework.annotation.CommonTrackerHandler;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.util.PatternMatcher;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Common implementation of {@link AbstractTrackerHandler} for {@code Luminance}-based trackers.
 */
@CommonTrackerHandler("Luminance")
@TrackerHandler(name = "PixelCove", url = "https://www.pixelcove.me/")
@TrackerHandler(name = "PornBay", url = "https://pornbay.org/")
public class LuminanceHandler extends AbstractTrackerHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public LuminanceHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    public By loginPageSelector() {
        return By.xpath("//div[@id='logo']/ul[1]/li[2]/a[1]");
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

    @Override
    protected By postLoginSelector() {
        return By.id("userinfo_username");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link LuminanceHandler}, we also need to redact a passkey {@link WebElement}. We find the element defining the user's passkey in the
     * stats element on the profile page. We redact this element by replacing all text with the prefix and
     * {@value PatternMatcher#DEFAULT_REDACTION_TEXT}.
     *
     * @see AbstractTrackerHandler#redactElements()
     * @see ScriptExecutor#redactInnerTextOf(WebElement, String)
     */
    @Override
    public int redactElements() {
        final WebElement passkeyElement = driver.findElement(passkeyElementSelector());
        final String passkeyRedactionText = "Passkey: " + PatternMatcher.DEFAULT_REDACTION_TEXT;
        scriptExecutor.redactInnerTextOf(passkeyElement, passkeyRedactionText);

        return 1 + super.redactElements();
    }

    /**
     * Utility function that calls {@link #redactElements()} from {@link AbstractTrackerHandler}. This will allow any implementations of this class to
     * use the original function.
     *
     * @return the number of {@link WebElement}s where the text has been redacted
     */
    protected int originalRedactElements() {
        return super.redactElements();
    }

    /**
     * Defines the {@link By} selector for the user's passkey on the user profile page.
     *
     * @return the {@link By} selector for the user's passkey
     */
    protected By passkeyElementSelector() {
        return By.xpath("//div[contains(@class, 'sidebar')]/div[8]/ul[1]/li[4]");
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            By.xpath("//ul[contains(@class, 'stats')]/li/a[1]"), // Email
            By.id("statuscont0") // IP address
        );
    }

    @Override
    protected By logoutButtonSelector() {
        // Highlight the nav bar to make the logout button interactable
        final By logoutParentSelector = By.xpath("//a[contains(@class, 'username')]");
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        scriptExecutor.moveTo(logoutParent);

        return By.xpath("//li[@id='nav_logout']/a[1]");
    }
}
