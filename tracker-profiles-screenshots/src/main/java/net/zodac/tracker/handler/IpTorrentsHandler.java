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
import net.zodac.tracker.util.PatternMatcher;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code IPTorrents} tracker.
 */
@TrackerHandler(name = "IPTorrents", url = "https://iptorrents.com/")
public class IpTorrentsHandler extends AbstractTrackerHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public IpTorrentsHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    protected By loginButtonSelector() {
        return By.xpath("//button[contains(@class, 'submit-btn') and @type='submit']");
    }

    @Override
    protected By postLoginSelector() {
        return By.xpath("//div[contains(@class, 'stats')]");
    }

    @Override
    protected By profilePageSelector() {
        return By.xpath("//a[contains(@class, 'uname')]");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link IpTorrentsHandler}, while we do not have any elements in {@link #getElementsPotentiallyContainingSensitiveInformation()}, we do
     * override {@link #redactElements()} in order to redact the passkey in the user profile. So we set this value to <code>true</code> to ensure
     * this redaction is performed.
     */
    @Override
    public boolean hasElementsNeedingRedaction() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link IpTorrentsHandler}, there is a table with our passkey. We
     * find the {@literal <}{@code tr}{@literal >} {@link WebElement} which has a {@literal <}{@code th}{@literal >} {@link WebElement} with the text
     * value <b>Passkey</b>. From this {@literal <}{@code tr}{@literal >}, we find the child {@literal <}{@code td}{@literal >}, which needs
     * its content redacted.
     *
     * @see AbstractTrackerHandler#redactElements()
     * @see ScriptExecutor#redactInnerTextOf(WebElement, String)
     */
    @Override
    public int redactElements() {
        final WebElement passkeyValueElement = driver.findElement(By.xpath("//tr[th[contains(normalize-space(), 'Passkey')]]/td[1]"));
        scriptExecutor.redactInnerTextOf(passkeyValueElement, PatternMatcher.DEFAULT_REDACTION_TEXT);

        return 1 + super.redactElements();
    }

    @Override
    protected By logoutButtonSelector() {
        return By.xpath("//button[div[normalize-space()='Log out']]");
    }
}
