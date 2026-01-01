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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Extension of the {@link Unit3dHandler} for the {@code Lat-Team} tracker.
 */
@TrackerHandler(name = "Lat-Team", url = "https://lat-team.com/")
public class LatTeamHandler extends Unit3dHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public LatTeamHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link LatTeamHandler}, unlike most other {@code UNIT3D}-based trackers, the user profile dropdown does not open when the mouse hovers
     * over it. Instead, we need to click it.
     *
     * @return {@link By} the selector for the logout link
     */
    @Override
    protected By logoutButtonSelector() {
        final By logoutParentSelector = By.xpath("//span[contains(@class, 'user-sidebar__toggle-label')]");
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        clickButton(logoutParent);

        return By.xpath("//button[contains(@class, 'user-sidebar__menu-link--logout')]");
    }
}
