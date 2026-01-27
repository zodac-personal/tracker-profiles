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
import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code SportsCult} tracker.
 */
@TrackerHandler(name = "SportsCult", url = "https://sportscult.org/")
public class SportsCultHandler extends AbstractTrackerHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public SportsCultHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    @Override
    protected By usernameFieldSelector() {
        return By.xpath("//input[@name='uid'][@type='text']");
    }

    @Override
    protected By passwordFieldSelector() {
        return By.xpath("//input[@name='pwd'][@type='password']");
    }

    @Override
    protected By loginButtonSelector() {
        return By.xpath("//input[@type='submit']");
    }

    @Override
    protected By postLoginSelector() {
        return By.xpath("//form[@name='jump1']");
    }

    @Override
    protected By profilePageSelector() {
        return By.xpath("//a[contains(@class, 'mainuser')]");
    }

    @Override
    protected By logoutButtonSelector() {
        return By.xpath("//div[@id='header']/table[1]//table[1]/tbody[1]/tr[1]/td[1]/a[1]");
    }
}
