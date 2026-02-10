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

import static net.zodac.tracker.framework.xpath.HtmlElement.a;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;

import java.util.Collection;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Extension of the {@link Unit3dHandler} for the {@code SeedPool} tracker.
 */
@TrackerHandler(name = "SeedPool", url = "https://seedpool.org/")
public class SeedPoolHandler extends Unit3dHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public SeedPoolHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link SeedPoolHandler}, unlike most other {@code UNIT3D}-based trackers, there is no link to the username available in the nav bar.
     * Instead, we must move the mouse over the profile icon in the nav bar. This will activate the dropdown menu and make the profile link button
     * interactive.
     *
     * @return {@link By} the selector for the profile link
     */
    @Override
    protected By profilePageSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(a, withClass("top-nav__username"))
            .build();
    }
}
