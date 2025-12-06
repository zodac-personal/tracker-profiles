/*
 * BSD Zero Clause License
 *
 * Copyright (c) 2024-2025 zodac.net
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
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code ABTorrents} tracker.
 */
@TrackerHandler(name = "ABTorrents", type = TrackerType.MANUAL, url = {
    "https://usefultrash.net/",
    "https://abtorrents.xyz/",
})
public class AbTorrentsHandler extends AbstractTrackerHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public AbTorrentsHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    protected By usernameFieldSelector() {
        return By.xpath("//input[@name='username' and @type='text']");
    }

    @Override
    protected By passwordFieldSelector() {
        return By.xpath("//input[@name='password' and @type='password']");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected By loginButtonSelector() {
        return By.xpath("//input[@name='submitme' and @type='submit' and @value='Login']");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link AbTorrentsHandler}, having any unread private messages means you are unable to search for any torrents. While this doesn't block
     * the profile page, we'll click the link to the inbox then open any unread PMs before continuing.
     */
    @Override
    protected void manualCheckAfterLoginClick(final String trackerName) {
        ScriptExecutor.explicitWait(WAIT_FOR_LOGIN_PAGE_LOAD);

        final By pmWarningSelector = By.xpath("//a[b[contains(@class, 'alert-warning') and contains(text(), 'New Private message')]]");
        final List<WebElement> privateMessageWarnings = driver.findElements(pmWarningSelector);

        if (privateMessageWarnings.isEmpty()) {
            LOGGER.debug("\t- No unread PMs");
            return;
        }

        LOGGER.trace("\t- Found some unread private messages, opening inbox");
        final WebElement privateMessageWarning = privateMessageWarnings.getFirst();
        clickButton(privateMessageWarning);

        final By unreadPmsSelector = By.xpath("//td[span[1][contains(text(), 'Unread')]]/a[1]");
        final List<WebElement> unreadPms = driver.findElements(unreadPmsSelector);
        LOGGER.debug("\t- {} unread PMs", unreadPms.size());

        for (final WebElement unreadPm : unreadPms) {
            clickButton(unreadPm);
            driver.navigate().back();
        }

        LOGGER.debug("\t\t- Unread PMs cleared");
    }

    @Override
    protected By postLoginSelector() {
        return By.id("base_usermenu");
    }

    @Override
    protected By profilePageSelector() {
        return By.xpath("//div[@id='base_usermenu']//div[1]//span[1]//a[1]");
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            By.xpath("//tr[td[1][contains(text(), 'Connectable')]]/td[2]//span") // Last connected IP address
        );
    }

    @Override
    protected By logoutButtonSelector() {
        ScriptExecutor.explicitWait(Duration.ofSeconds(1L)); // Wait for the logout button to become visible and clickable again after scrolling
        return By.id("logoff");
    }
}
