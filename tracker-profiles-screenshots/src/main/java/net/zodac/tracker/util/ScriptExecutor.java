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

package net.zodac.tracker.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Utility class used to execute scripts on a web page.
 */
public class ScriptExecutor {

    private static final Duration DEFAULT_WAIT_FOR_ALERT = Duration.of(2L, ChronoUnit.SECONDS);
    private static final Duration DEFAULT_WAIT_FOR_MOUSE_MOVE = Duration.of(300L, ChronoUnit.MILLIS);
    private static final Duration DEFAULT_WAIT_FOR_PAGE_LOAD = Duration.of(1_000L, ChronoUnit.MILLIS);
    private static final Logger LOGGER = LogManager.getLogger();

    private final RemoteWebDriver driver;

    /**
     * Constructor that takes in a {@link RemoteWebDriver}.
     *
     * @param driver the {@link RemoteWebDriver}
     */
    public ScriptExecutor(final RemoteWebDriver driver) {
        this.driver = driver;
    }

    /**
     * Finds an alert and accepts it.
     */
    public void acceptAlert() {
        final Wait<WebDriver> wait = new WebDriverWait(driver, DEFAULT_WAIT_FOR_ALERT)
            .ignoring(NoSuchElementException.class);
        final Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        if (alert != null) {
            LOGGER.trace("Accepting alert pop-up");
            alert.accept();
        } else {
            LOGGER.warn("Expected alert not found, not attempting to click anything");
        }
    }

    /**
     * Disables scrolling on the current web page, to remove the scrollbar from the screenshot.
     */
    public void disableScrolling() {
        LOGGER.trace("Disabling scrolling on page to remove scrollbar");
        driver.executeScript("document.body.style.overflow = 'hidden'");
    }

    /**
     * Some web pages may have 'overflow' set to 'hidden', which can disable scrolling. This function will override the configuration of the web page
     * to enable scrolling again.
     *
     * @param elementToOverride the element that needs to be overridden to allow scrolling (usually 'body')
     */
    public void enableScrolling(final String elementToOverride) {
        LOGGER.trace("Enabling scrolling on page");
        driver.executeScript(String.format("document.%s.style.height = 'auto';", elementToOverride));
        driver.executeScript(String.format("document.%s.style.overflowY = 'visible';", elementToOverride));
    }

    /**
     * Performs a {@link Thread#sleep(Duration)} for the specified {@link Duration}.
     *
     * @param sleepTime the time to wait
     * @param reason the reason for sleeping
     */
    public static void explicitWait(final Duration sleepTime, final String reason) {
        try {
            LOGGER.trace("Sleeping for {}, waiting for {}", sleepTime, reason);
            Thread.sleep(sleepTime);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Highlight's an {@link WebElement} on the web page. Creates a 3px solid red border around the {@link WebElement}.
     *
     * @param element the {@link WebElement} to highlight
     */
    public void highlightElement(final WebElement element) {
        updateCss(element, "border", "3px solid red");
    }

    /**
     * Updates the {@link WebElement} and unfixes it from its pinned position.
     *
     * @param element the {@link WebElement} to unfix
     */
    public void makeUnfixed(final WebElement element) {
        updateCss(element, "position", "static");
    }

    private void updateCss(final WebElement element, final String propertyName, final String propertyValue) {
        LOGGER.trace("Updating CSS of element '{}', property '{}' to value: '{}'", element, propertyName, propertyValue);
        final String script = String.format("arguments[0].style.%s = '%s';", propertyName, propertyValue);
        driver.executeScript(script, element);
    }

    /**
     * Moves the mouse cursor to the provided {@link WebElement}.
     *
     * @param element the {@link WebElement} to move to
     */
    public void moveTo(final WebElement element) {
        LOGGER.trace("Moving cursor to element {}", element);
        final Actions actions = new Actions(driver);
        actions.moveToElement(element).perform();
        explicitWait(DEFAULT_WAIT_FOR_MOUSE_MOVE, "cursor to move to element");
    }

    /**
     * Moves the mouse cursor to the provided {@link WebElement}.
     *
     * @param x positive pixel value along horizontal axis in viewport (numbers increase going right)
     * @param y positive pixel value along vertical axis in viewport (numbers increase going down)
     */
    public void moveTo(final int x, final int y) {
        LOGGER.trace("Moving cursor to ({},{})", x, y);
        final Actions actions = new Actions(driver);
        actions.moveToLocation(x, y).perform();
        explicitWait(DEFAULT_WAIT_FOR_MOUSE_MOVE, "cursor to move to coordinates");
    }

    /**
     * Moves the mouse cursor the origin of the web page; the top-left corner.
     */
    public void moveToOrigin() {
        moveTo(0, 0);
    }

    /**
     * Remove an HTML attribute from the {@link WebElement}.
     *
     * @param element       the {@link WebElement} to update
     * @param attributeName the HTML attribute name
     */
    public void removeAttribute(final WebElement element, final String attributeName) {
        LOGGER.trace("Removing attribute '{}' from {}", attributeName, element);
        final String script = String.format("arguments[0].removeAttribute('%s');", attributeName);
        driver.executeScript(script, element);
    }

    /**
     * Scrolls the page back to the top of the screen.
     */
    public void scrollToTheTop() {
        driver.executeScript("window.scrollTo(0, 0);");
        explicitWait(Duration.ofSeconds(1L), "page to scroll to the top");
    }

    /**
     * Stops the loading of the current web page.
     */
    public void stopPageLoad() {
        LOGGER.trace("Stopping page load");
        driver.executeScript("window.stop();");
    }

    /**
     * Waits for the page that the {@link WebDriver} is loading to find the wanted {@link WebElement}. If the {@code timeout} {@link Duration} is
     * exceeded, the execution will continue.
     *
     * @param selector the {@link By} selector for the wanted {@link WebElement}
     * @param timeout  the maximum {@link Duration} to wait
     */
    public void waitForElementToAppear(final By selector, final Duration timeout) {
        try {
            LOGGER.trace("Waiting {} for [{}] to appear", timeout, selector);
            final Wait<WebDriver> wait = new WebDriverWait(driver, timeout);
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(selector));
        } catch (final TimeoutException e) {
            LOGGER.trace("Element didn't appear, page source: {}", driver.getPageSource());
            throw e;
        }
    }

    /**
     * Waits for the page that the {@link RemoteWebDriver} is loading to completely load. If the {@code timeout} {@link Duration} is exceeded, the
     * execution will continue.
     *
     * @param timeout the maximum {@link Duration} to wait
     */
    public void waitForPageToLoad(final Duration timeout) {
        explicitWait(DEFAULT_WAIT_FOR_PAGE_LOAD, "page to load");

        try {
            LOGGER.trace("Waiting {} for page to load", timeout);
            final Wait<WebDriver> wait = new WebDriverWait(driver, timeout);
            wait.until(_ -> "complete".equals(driver.executeScript("return document.readyState")));
        } catch (final TimeoutException e) {
            LOGGER.debug("Page didn't load, page source: {}", driver.getPageSource());
            throw e;
        }
    }
}
