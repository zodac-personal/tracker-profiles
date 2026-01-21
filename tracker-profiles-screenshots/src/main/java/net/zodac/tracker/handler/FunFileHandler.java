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
import net.zodac.tracker.util.PatternMatcher;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code FunFile} tracker.
 */
@TrackerHandler(name = "FunFile", url = "https://www.funfile.org/")
public class FunFileHandler extends AbstractTrackerHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public FunFileHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    protected By passwordFieldSelector() {
        return By.xpath("//input[@name='password']");
    }

    @Override
    protected By loginButtonSelector() {
        return By.xpath("//input[@name='login' and @type='submit']");
    }

    @Override
    protected By postLoginSelector() {
        return By.id("clock");
    }

    @Override
    protected By profilePageSelector() {
        return By.xpath("//div[@id='avatar']/a[1]");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link FunFileHandler}, while we can redact the IP address using {@link #getElementsPotentiallyContainingSensitiveInformation()}, that
     * table entry also contains information on the user's ISP, so we redact that IP/ISP entry explicitly.
     *
     * @see AbstractTrackerHandler#redactElements()
     * @see ScriptExecutor#redactInnerTextOf(WebElement, String)
     */
    @Override
    public int redactElements() {
        final WebElement ipAndIspElement = driver.findElement(By.xpath("//td[contains(@class, 'mf_content')]/table/tbody/tr[td[contains(normalize-space(), 'IP')]]/td[2]"));
        scriptExecutor.redactInnerTextOf(ipAndIspElement, PatternMatcher.DEFAULT_REDACTION_TEXT);

        return 1 + super.redactElements();
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            By.xpath("//td[contains(@class, 'mf_content')]/table/tbody/tr/td/a") // Email
        );
    }

    @Override
    protected By logoutButtonSelector() {
        return By.xpath("//a[font[b[contains(normalize-space(), 'Logout')]]]");
    }
}
