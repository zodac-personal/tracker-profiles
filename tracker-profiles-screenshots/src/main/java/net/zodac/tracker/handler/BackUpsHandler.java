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
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Extension of the {@link TsSpecialEditionHandler} for the {@code BackUps} tracker.
 */
@TrackerHandler(name = "BackUps", url = "https://back-ups.me/")
public class BackUpsHandler extends TsSpecialEditionHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public BackUpsHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    public By loginPageSelector() {
        return By.xpath("//a[contains(normalize-space(), 'Login')]");
    }

    @Override
    protected By usernameFieldSelector() {
        return By.xpath("//input[@name='username' and @type='text']");
    }

    @Override
    protected By passwordFieldSelector() {
        return By.xpath("//input[@name='password' and @type='password']");
    }

    @Override
    protected By loginButtonSelector() {
        return By.xpath("//input[@value='LOGIN' and @type='submit']");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link BackUpsHandler}, having any unread private messages means you are redirected to your PM inbox. We'll check if this is the case, and
     * then navigate to the home page.
     */
    @Override
    protected void manualCheckAfterLoginClick(final String trackerName) {
        ScriptExecutor.explicitWait(WAIT_FOR_LOGIN_PAGE_LOAD);

        final String currentTitle = driver.getTitle();
        if (currentTitle == null || !currentTitle.contains("Private Messages in Folder: Inbox")) {
            LOGGER.debug("\t- No unread PMs");
            return;
        }

        LOGGER.debug("\t\t- Redirected to inbox due to unread PMs, navigating back to the home page");
        final WebElement homePageLink = driver().findElement(By.xpath("//div[@id='menu']//a[contains(normalize-space(), 'Home')]"));
        clickButton(homePageLink);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link BackUpsHandler}, after clicking the logout button, a Javascript alert appears, which must be accepted.
     */
    @Override
    public void logout() {
        final By logoutButtonSelector = logoutButtonSelector();
        scriptExecutor.waitForElementToAppear(logoutButtonSelector, DEFAULT_WAIT_FOR_PAGE_LOAD);
        final WebElement logoutButton = driver.findElement(logoutButtonSelector);
        clickButton(logoutButton);

        // After clicking logout, an alert appears - find and click 'Yes'
        scriptExecutor.acceptAlert();

        scriptExecutor.waitForPageToLoad(DEFAULT_WAIT_FOR_PAGE_LOAD);
        scriptExecutor.waitForElementToAppear(postLogoutElementSelector(), DEFAULT_WAIT_FOR_PAGE_LOAD);
    }

    @Override
    protected By logoutButtonSelector() {
        // Click the user dropdown menu bar to make the logout button interactable
        final By logoutParentSelector = By.id("quickprofileview");
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        clickButton(logoutParent);

        return By.xpath("//div[contains(@class, 'qactions')]/a[2]");
    }
}
