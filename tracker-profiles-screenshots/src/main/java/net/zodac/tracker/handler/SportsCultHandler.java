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
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
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
        return By.xpath("//form[@name='login']//table[1]//table[1]/tbody/tr/td[2]/input[1]");
    }

    @Override
    protected By passwordFieldSelector() {
        return By.xpath("//form[@name='login']//table[1]//table[1]/tbody/tr/td[4]/input[1]");
    }

    @Override
    protected By loginButtonSelector() {
        return By.xpath("//form[@name='login']//table[1]//table[1]/tbody/tr/td[5]/input[1]");
    }

    @Override
    protected By postLoginSelector() {
        return By.xpath("//form[@name='jump1']");
    }

    @Override
    protected By profilePageSelector() {
        return By.xpath("//a[contains(@class, 'mainuser') and contains(normalize-space(), 'My Panel')]");
    }

    @Override
    protected By logoutButtonSelector() {
        return By.xpath("//a[contains(normalize-space(), '(Logout)')]");
    }
}
