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

package net.zodac.tracker.framework.driver.extension;

import static net.zodac.tracker.framework.xpath.HtmlElement.button;
import static net.zodac.tracker.framework.xpath.HtmlElement.div;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.time.Duration;
import java.util.List;
import net.zodac.tracker.framework.xpath.NamedHtmlElement;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.ScriptExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Google Chrome {@link Extension} for {@code uBlock Origin Lite}, used as an ad-blocker for websites.
 */
public class UblockOriginLiteExtension implements Extension {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String id() {
        return "ddkjiahejlhfcafbddmgiahcphecmpfh";
    }

    @Override
    public String path() {
        return "/app/ublock_origin_lite.crx";
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link UblockOriginLiteExtension}, we configure the {@link Extension} by increasing the ad-blocker from the default settings. We make the
     * following changes:
     *
     * <ol>
     *     <li>Set filtering mode to 'Complete'</li>
     *     <li>Enable all 'Miscellaneous' filter lists</li>
     *     <li>Enable all 'Region' filter lists</li>
     * </ol>
     */
    @Override
    public void configure(final RemoteWebDriver driver, final ScriptExecutor scriptExecutor) {
        try {
            LOGGER.info("\t- Configuring ad-blocker");
            openExtensionConfigurationPage(driver, id());

            LOGGER.debug("\t\t- Setting filtering mode");
            settingFilteringMode(driver);

            LOGGER.debug("\t\t- Opening filter lists page");
            openFilterListsPage(driver, scriptExecutor);

            LOGGER.debug("\t\t- Enabling all miscellaneous filters");
            enableAllMiscellaneousFilters(driver, scriptExecutor);

            LOGGER.debug("\t\t- Expanding region filter lists");
            expandRegionFiltersList(driver, scriptExecutor);

            LOGGER.debug("\t\t- Enabling all region filters");
            enableAllRegionFilters(driver, scriptExecutor);
        } catch (final Exception e) {
            LOGGER.debug("Error configuring {}", UblockOriginLiteExtension.class.getSimpleName(), e);
            LOGGER.warn("Error configuring {}: {}", UblockOriginLiteExtension.class.getSimpleName(), e.getMessage());
        }
    }

    private static void openExtensionConfigurationPage(final RemoteWebDriver driver, final String id) {
        driver.navigate().to(String.format("chrome-extension://%s/dashboard.html", id));
        ScriptExecutor.explicitWait(Duration.ofSeconds(1L), "configuration page to open");
    }

    // Set filtering mode to 'Complete' option
    private static void settingFilteringMode(final RemoteWebDriver driver) {
        final By filteringModeSelector = XpathBuilder
            .from(div, withId("defaultFilteringMode"))
            .child(NamedHtmlElement.of("label"), atIndex(3))
            .child(div, atIndex(3))
            .build();
        final WebElement filteringMode = driver.findElement(filteringModeSelector);
        filteringMode.click();
    }

    private static void openFilterListsPage(final RemoteWebDriver driver, final ScriptExecutor scriptExecutor) {
        final By filterListSelector = XpathBuilder
            .from(NamedHtmlElement.of("nav"), withId("dashboard-nav"))
            .child(button, atIndex(2))
            .build();
        final WebElement filterList = driver.findElement(filterListSelector);
        filterList.click();
        scriptExecutor.waitForPageToLoad(Duration.ofSeconds(1L));
    }

    private static void enableAllMiscellaneousFilters(final RemoteWebDriver driver, final ScriptExecutor scriptExecutor) {
        final By miscFilterCheckboxesSelector = XpathBuilder
            .from(div, withId("lists"))
            .child(div, atIndex(1))
            .child(div, atIndex(6))
            .child(div, atIndex(2))
            .child(div)
            .child(span, atIndex(1))
            .child(NamedHtmlElement.of("label"), atIndex(1))
            .child(span, atIndex(1))
            .descendant(input, withType("checkbox"))
            .build();

        enableAllCheckBoxes(driver, scriptExecutor, miscFilterCheckboxesSelector);
    }

    private static void expandRegionFiltersList(final RemoteWebDriver driver, final ScriptExecutor scriptExecutor) {
        final By regionsListSelector = XpathBuilder
            .from(div, withId("lists"))
            .child(div, atIndex(1))
            .child(div, atIndex(7))
            .child(div, atIndex(1))
            .child(span, atIndex(2))
            .build();
        final WebElement regionsList = driver.findElement(regionsListSelector);
        scriptExecutor.scrollToElement(regionsList);
        regionsList.click();
        LOGGER.trace("Clicking {} to expand list", regionsList);
        ScriptExecutor.explicitWait(Duration.ofSeconds(1L), "region filter lists to expand");
    }

    private static void enableAllRegionFilters(final RemoteWebDriver driver, final ScriptExecutor scriptExecutor) {
        final By regionFilterCheckboxesSelector = XpathBuilder
            .from(div, withId("lists"))
            .child(div, atIndex(1))
            .child(div, atIndex(7))
            .child(div, atIndex(2))
            .child(div)
            .child(span, atIndex(1))
            .child(NamedHtmlElement.of("label"), atIndex(1))
            .child(span, atIndex(1))
            .descendant(input, withType("checkbox"))
            .build();

        enableAllCheckBoxes(driver, scriptExecutor, regionFilterCheckboxesSelector);
    }

    private static void enableAllCheckBoxes(final RemoteWebDriver driver, final ScriptExecutor scriptExecutor, final By checkboxesSelector) {
        final List<WebElement> checkboxes = driver.findElements(checkboxesSelector).stream().toList();
        LOGGER.debug("\t\t\t- Found {} checkboxes", checkboxes.size());
        for (int i = 0; i < checkboxes.size(); i++) {
            final WebElement checkbox = checkboxes.get(i);
            scriptExecutor.scrollToElement(checkbox);
            scriptExecutor.scroll(0, -150);

            LOGGER.trace("Clicking checkbox {}: {}", (i + 1), checkbox.getText());
            checkbox.click();
        }
    }
}
