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
import static net.zodac.tracker.framework.xpath.HtmlElement.div;
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.ul;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Extension of the {@link LuminanceHandler} for the {@code Empornium} tracker.
 */
@TrackerHandler(name = "Empornium", url = {
    "https://www.empornium.sx/",
    "https://www.emparadise.rs/"
})
public class EmporniumHandler extends LuminanceHandler {

    @Override
    public By loginPageSelector() {
        return XpathBuilder
            .from(div, withId("menu"))
            .child(ul, atIndex(1))
            .child(li, atIndex(2))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        return By.id("stats_block");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link EmporniumHandler}, we close additional sections on the profile page prior to screenshotting. Continues if the section does not
     * exist for the user's profile. The sections to close are:
     *
     * <ul>
     *     <li>Recent snatches</li>
     *     <li>Collages</li>
     *     <li>Uploaded torrents</li>
     * </ul>
     */
    @Override
    protected void additionalActionOnProfilePage() {
        // Reload the page, to ensure the section closing works (JS may have been cancelled earlier)
        driver.navigate().refresh();
        scriptExecutor.waitForPageToLoad(DEFAULT_WAIT_FOR_PAGE_LOAD);

        final List<By> toggleSelectors = List.of(
            By.id("recentsnatchesbutton"),  // Recent snatches
            By.id("collagesbutton"),        // Collages
            By.id("submitbutton")           // Uploaded torrents
        );

        for (final By toggleSelector : toggleSelectors) {
            final Collection<WebElement> sectionToggles = driver.findElements(toggleSelector);
            for (final WebElement sectionToggle : sectionToggles) {
                // Only click the toggle if it is already open
                if (sectionToggle.getText().contains("Hide")) {
                    LOGGER.debug("\t\t- Closing section {}", toggleSelector);
                    clickButton(sectionToggle);
                    ScriptExecutor.explicitWait(DEFAULT_WAIT_FOR_TRANSITIONS);
                }
            }
        }
    }

    @Override
    public int redactElements() {
        return super.originalRedactElements();
    }
}
