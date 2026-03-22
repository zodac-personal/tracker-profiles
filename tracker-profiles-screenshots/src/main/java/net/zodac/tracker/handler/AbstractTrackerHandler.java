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

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.zodac.tracker.app.ScreenshotOrchestrator;
import net.zodac.tracker.framework.TrackerDefinition;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.driver.extension.Extension;
import net.zodac.tracker.framework.driver.java.JavaWebDriverFactory;
import net.zodac.tracker.framework.driver.python.PythonWebDriverFactory;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.handler.definition.HasCloudflareCheck;
import net.zodac.tracker.handler.definition.NeedsExplicitTranslation;
import net.zodac.tracker.handler.definition.TrackerTimings;
import net.zodac.tracker.handler.definition.UsesExtensions;
import net.zodac.tracker.redaction.RedactionBuffer;
import net.zodac.tracker.redaction.Redactor;
import net.zodac.tracker.util.BrowserInteractionHelper;
import net.zodac.tracker.util.TextSearcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Abstract class used to define a {@link AbstractTrackerHandler}. All implementations will be used by {@link ScreenshotOrchestrator}, if the tracker
 * is included in the tracker input file. This class lists the high-level methods required for {@link ScreenshotOrchestrator} to be able to
 * successfully generate a screenshot for a given tracker.
 *
 * <p>
 * Since each tracker website has its own UI and own page structure, each implementation of {@link AbstractTrackerHandler} will contain the
 * tracker-specific {@code Selenium} logic to perform the UI actions.
 */
public abstract class AbstractTrackerHandler implements AutoCloseable, TrackerTimings {

    /**
     * The logger instance.
     */
    protected static final Logger LOGGER = LogManager.getLogger();

    /**
     * The {@link RemoteWebDriver} instance used to load web pages and perform UI actions.
     */
    @SuppressWarnings("NullAway") // Will be set in the configure() method
    protected RemoteWebDriver driver;

    /**
     * The {@link BrowserInteractionHelper} instance to perform specific actions for each {@link AbstractTrackerHandler} implementation.
     */
    @SuppressWarnings("NullAway") // Will be set in the configure() method
    protected BrowserInteractionHelper browserInteractionHelper;

    /**
     * The {@link TrackerDefinition}.
     */
    @SuppressWarnings("NullAway") // Will be set in the configure() method
    protected TrackerDefinition trackerDefinition;

    /**
     * We use a no-arg constructor to instantiate the {@link AbstractTrackerHandler} to avoid needing to define a constructor for each implementation.
     * However, we still need to configure the {@link AbstractTrackerHandler} with details for the tracker for execution, so we overwrite the default
     * values that were already set.
     *
     * @param trackerDefinition the {@link TrackerDefinition} for this {@link AbstractTrackerHandler}
     */
    public void configure(final TrackerDefinition trackerDefinition) {
        this.trackerDefinition = trackerDefinition;
        final List<Extension> extensions = this instanceof UsesExtensions tracerExtensions ? tracerExtensions.requiredExtensions() : List.of();
        driver = createRemoteWebDriver(trackerDefinition.type(), extensions);
        browserInteractionHelper = new BrowserInteractionHelper(driver);
    }

    /**
     * Navigates to the home page of the tracker. Waits {@link #pageLoadDuration()} for the page to finish loading.
     */
    public void openTracker() {
        boolean successfulConnection = false;

        for (final String trackerUrl : trackerDefinition.urls()) {
            if (successfulConnection) {
                // A previous URL successfully connected, no need to try another
                break;
            }

            try {
                LOGGER.info("\t\t- '{}'", trackerUrl);
                driver.manage().timeouts().pageLoadTimeout(maximumLinkResolutionDuration());
                driver.navigate().to(trackerUrl);

                // Explicit check for Cloudflare Error 523 if a site is unavailable
                final String bodyText = driver.findElement(By.tagName("body")).getText();
                if (bodyText.contains("HTTP ERROR 523") || bodyText.contains("Error code 523")) {
                    LOGGER.warn("\t\t- Unable to connect: Cloudflare Error 523 (Origin is unreachable)");
                } else {
                    successfulConnection = true;
                    browserInteractionHelper.waitForPageToLoad(pageLoadDuration());
                }
            } catch (final WebDriverException e) {
                // If website can't be resolved, assume the site is down and attempt the next URL (if any), else rethrow exception
                if (e.getMessage() != null && e.getMessage().contains("ERR_NAME_NOT_RESOLVED")) {
                    final String errorMessage = e.getMessage().split("\n")[0];
                    LOGGER.warn("\t\t- Unable to connect: {}", errorMessage);
                } else {
                    throw e;
                }
            }
        }

        // If all possible URLs have been attempted but no connection occurred, assume the website is down
        if (!successfulConnection) {
            throw new IllegalStateException(
                String.format("Tracker unavailable, unable to connect to any URL for '%s': %s", trackerDefinition.name(), trackerDefinition.urls()));
        }
    }

    /**
     * For some trackers the home page does not automatically redirect to the login page. In these cases, we define a {@link By} selector of the
     * {@link WebElement} to navigate to the login page, for trackers. Is {@code null} by default as we assume this navigation is unnecessary. Should
     * be overridden otherwise.
     *
     * @return the login page {@link By} selector
     */
    @Nullable
    protected By loginPageSelector() {
        return null;
    }

    /**
     * For some trackers the home page does not automatically redirect to the login page. In these cases, we need to explicitly click on the login
     * link to redirect. We'll only do this navigation if {@link #loginPageSelector()} is not {@code null}.
     *
     * @param trackerName the name of the tracker
     */
    public void navigateToLoginPage(final String trackerName) {
        LOGGER.debug("\t- Navigating to login page");
        final By loginLinkSelector = loginPageSelector();

        if (loginLinkSelector != null) {
            final WebElement loginLink = driver.findElement(loginLinkSelector);
            clickButton(loginLink);
            cloudflareCheck(trackerName);
            browserInteractionHelper.waitForElementToAppear(usernameFieldSelector(), pageLoadDuration());
        } else {
            cloudflareCheck(trackerName);
        }
    }

    /**
     * For this {@link AbstractTrackerHandler} implementation, there is a Cloudflare check protecting the login page. This verification check must be
     * passed to proceed.
     *
     * <p>
     * Manual user interactions:
     * <ol>
     *     <li>Pass the Cloudflare verification check</li>
     * </ol>
     *
     * @param trackerName the name of the tracker
     */
    // TODO: Can this button be automatically clicked? If the box is always in the same place, move the mouse and click?
    private void cloudflareCheck(final String trackerName) {
        if (!(this instanceof HasCloudflareCheck trackerHasCloudflareCheck)) {
            return;
        }

        LOGGER.debug("\t- Performing Cloudflare verification check");
        browserInteractionHelper.waitForPageToLoad(pageLoadDuration());
        LOGGER.info("\t\t >>> Waiting for user to pass the Cloudflare verification");

        final WebElement cloudflareElement = driver.findElement(trackerHasCloudflareCheck.cloudflareSelector());
        browserInteractionHelper.highlightElement(cloudflareElement);
        DisplayUtils.userInputConfirmation(trackerName, "Pass the Cloudflare verification");
    }

    /**
     * Enters the user's credential and logs in to the tracker. Waits {@link #pageLoadDuration()} for the page to finish loading.
     *
     * @param username    the user's username for the tracker
     * @param password    the user's password for the tracker
     * @param trackerName the name of the tracker
     */
    public void login(final String username, final String password, final String trackerName) {
        LOGGER.trace("Logging in to tracker '{}'", trackerName);
        browserInteractionHelper.waitForPageToLoad(pageLoadDuration());

        try {
            LOGGER.trace("Entering username");
            browserInteractionHelper.waitForElementToAppear(usernameFieldSelector(), pageLoadDuration());
            final WebElement usernameField = browserInteractionHelper.waitForElementToBeInteractable(usernameFieldSelector(), pageLoadDuration());
            usernameField.clear();
            usernameField.sendKeys(username);
        } catch (final TimeoutException e) {
            throw new TimeoutException("Unable to find username field, tracker may not have loaded", e);
        }

        LOGGER.trace("Entering password");
        final By passwordFieldSelector = passwordFieldSelector();
        browserInteractionHelper.waitForElementToAppear(passwordFieldSelector, pageLoadDuration());
        final WebElement passwordField = browserInteractionHelper.waitForElementToBeInteractable(passwordFieldSelector, pageLoadDuration());
        passwordField.clear();
        passwordField.sendKeys(password);

        manualCheckBeforeLoginClick();

        // TODO: Check if the web page has changed (user clicked login during manual operation), and skip this?
        //       Maybe even add a listener to the timer code and wait for a page update?
        final By loginButtonSelector = loginButtonSelector();
        if (loginButtonSelector != null) {
            browserInteractionHelper.waitForElementToAppear(loginButtonSelector, pageLoadDuration());
            final WebElement loginButton = browserInteractionHelper.waitForElementToBeInteractable(loginButtonSelector, pageLoadDuration());
            LOGGER.trace("Clicking login button: {}", loginButton);
            clickButton(loginButton);
        }
        manualCheckAfterLoginClick();

        try {
            final By postLoginSelector = postLoginSelector();
            LOGGER.trace("Logged in, waiting for post-login selector: {}", postLoginSelector);
            browserInteractionHelper.waitForElementToAppear(postLoginSelector, pageLoadDuration());
        } catch (final TimeoutException e) {
            throw new TimeoutException("Timed out waiting for post-login selector, cannot confirm login was successful", e);
        }
    }

    /**
     * Defines the {@link By} selector of the {@link WebElement} where the username is entered to log in to the tracker.
     *
     * <p>
     * By default, we assume the username field has an {@link By#id(String)} of <b>username</b>. Should be overridden otherwise.
     *
     * @return the username field {@link By} selector
     */
    protected By usernameFieldSelector() {
        return By.id("username");
    }

    /**
     * Defines the {@link By} selector of the {@link WebElement} where the password is entered to log in to the tracker.
     *
     * <p>
     * By default, we assume the password field has an {@link By#id(String)} of <b>password</b>. Should be overridden otherwise.
     *
     * @return the password field {@link By} selector
     */
    protected By passwordFieldSelector() {
        return By.id("password");
    }

    /**
     * Pauses execution of the {@link AbstractTrackerHandler} prior after the first login attempt, generally for trackers that require an input prior
     * to clicking the login button.
     *
     * <p>
     * Where possible, the element to be interacted with will be highlighted in the browser.
     *
     * @see BrowserInteractionHelper#highlightElement(WebElement)
     */
    protected void manualCheckBeforeLoginClick() {
        // Do nothing by default
    }

    /**
     * Defines the {@link By} selector of the {@link WebElement} of the login button. Can be {@link Nullable} if there is no login button, and a user
     * interaction is required instead,
     *
     * @return the login button {@link By} selector
     */
    @Nullable // TODO: I think nothing sets this to null anymore - confirm and remove
    protected By loginButtonSelector() {
        return By.id("login-button");
    }

    /**
     * Pauses execution of the {@link AbstractTrackerHandler} prior after the first login attempt, generally for trackers that require a second input
     * after clicking the login button.
     *
     * <p>
     * Where possible, the element to be interacted with will be highlighted in the browser.
     *
     * @see BrowserInteractionHelper#highlightElement(WebElement)
     */
    protected void manualCheckAfterLoginClick() {
        // Do nothing by default
    }

    /**
     * Defines the {@link By} selector of the {@link WebElement} used to confirm that the user has successfully logged in.
     *
     * <p>
     * By default, this will use {@link #profilePageSelector()}, but if that selector requires some additional work (reloading the page, or
     * hovering/clicking another element to open a drop-down menu), then a simpler one should be used for this check.
     *
     * @return the post-login {@link By} selector
     */
    protected By postLoginSelector() {
        return profilePageSelector();
    }

    /**
     * Once logged in, navigates to the user's profile page on the tracker. Waits {@link #pageLoadDuration()} for the page to finish
     * loading.
     */
    public void openProfilePage() {
        LOGGER.trace("Opening profile page");
        browserInteractionHelper.waitForPageToLoad(pageTransitionsDuration());

        final WebElement profilePageLink = driver.findElement(profilePageSelector());
        browserInteractionHelper.removeAttribute(profilePageLink, "target"); // Removing 'target="_blank"', to ensure link opens in same tab
        clickButton(profilePageLink);

        try {
            LOGGER.debug("\t\t- Waiting to confirm user profile page has loaded successfully");
            browserInteractionHelper.waitForPageToLoad(pageLoadDuration());
            browserInteractionHelper.waitForElementToAppear(profilePageContentSelector(), pageLoadDuration());
            browserInteractionHelper.moveToOrigin();
            additionalActionOnProfilePage();
        } catch (final TimeoutException e) {
            throw new TimeoutException("Unable to find user profile content, profile page may not have loaded", e);
        }
    }

    /**
     * Defines the {@link By} selector of the {@link WebElement} that links to the user's profile page.
     *
     * @return the profile page link {@link By} selector
     */
    protected By profilePageSelector() {
        return XpathBuilder
            .from(a, withClass("username"))
            .build();
    }

    /**
     * Defines the {@link By} selector of a {@link WebElement} used to confirm that the user's details have loaded on the profile page.
     *
     * @return the profile page content {@link By} selector
     */
    protected abstract By profilePageContentSelector();

    /**
     * For certain trackers, additional actions may need to be performed after opening the profile page, but prior to the page being redacted and
     * screenshot. This might be that the page is considered 'loaded' by
     * {@link BrowserInteractionHelper#waitForPageToLoad(Duration)}, but the required {@link WebElement}s are not all
     * on the screen, or that some {@link WebElement}s may need to be interacted with prior to the screenshot.
     *
     * <p>
     * This method should be overridden as required.
     */
    // TODO: Move to interface
    protected void additionalActionOnProfilePage() {
        // Do nothing by default
    }

    /**
     * Reloads the current profile page in the browser, restoring the page to its original state (clearing any DOM mutations from redaction). Waits
     * {@link #pageLoadDuration()} for the page to finish loading, then re-runs any {@link #additionalActionOnProfilePage()}.
     */
    public void reloadProfilePage() {
        LOGGER.trace("Reloading profile page to restore original state");
        driver.navigate().refresh();
        browserInteractionHelper.waitForPageToLoad(pageLoadDuration());

        browserInteractionHelper.waitForElementToAppear(profilePageContentSelector(), pageLoadDuration());
        additionalActionOnProfilePage();
    }

    /**
     * Checks if the {@link AbstractTrackerHandler} has any sensitive information that requires redaction of any elements.
     *
     * @return <code>true</code> if there are elements in need of redaction
     */
    public final boolean hasSensitiveInformation() {
        return !emailElements().isEmpty()
            || !ipAddressElements().isEmpty()
            || !passkeyElements().isEmpty()
            || !sensitiveElements().isEmpty();
    }

    /**
     * Retrieves a {@link Collection} of {@link WebElement}s from the user's profile page, where the inner text needs to be redacted. This is used for
     * {@link WebElement}s that has sensitive information (like an IP address), which should not be visible in the screenshot. Once found, the text
     * in the {@link WebElement}s is redacted.
     *
     * @param redactor the {@link Redactor} to redact the sensitive information
     * @return the number of {@link WebElement}s where the text has been redacted
     * @see Redactor
     */
    // TODO: Add other redaction types (country, flag, avatar, gender, BT client, BT port, age/birthday)
    // TODO: Move redaction methods to interfaces
    public int redactElements(final Redactor redactor) {
        LOGGER.trace("Redacting elements");
        return redactEmailElements(redactor)
            + redactIpAddressElements(redactor)
            + redactPasskeyElements(redactor)
            + redactSensitiveElements(redactor);
    }

    /**
     * A {@link Collection} of {@link By} selectors for the user's visible email address on the profile page.
     *
     * @return {@link By} selectors for email HTML elements
     */
    protected Collection<By> emailElements() {
        return List.of();
    }

    /**
     * The {@link RedactionBuffer} applied when redacting email elements. Override to adjust the buffer for a specific tracker.
     *
     * @return the {@link RedactionBuffer} for email redaction
     */
    protected RedactionBuffer emailElementBuffer() {
        return RedactionBuffer.DEFAULT;
    }

    private int redactEmailElements(final Redactor redactor) {
        LOGGER.debug("\t\t- Redacting email elements");
        return emailElements()
            .stream()
            .flatMap(rootSelector -> driver.findElements(rootSelector).stream())
            .filter(element -> TextSearcher.hasEmailAddress(element.getText(), browserInteractionHelper.getValue(element)))
            .mapToInt(element -> {
                redactor.redactEmail(element, emailElementBuffer());
                return 1;
            })
            .sum();
    }

    /**
     * A {@link Collection} of {@link By} selectors for the user's visible IP address on the profile page.
     *
     * @return {@link By} selectors for IP address HTML elements
     */
    protected Collection<By> ipAddressElements() {
        return List.of();
    }

    /**
     * The {@link RedactionBuffer} applied when redacting IP address elements. Override to adjust the buffer for a specific tracker.
     *
     * @return the {@link RedactionBuffer} for IP address redaction
     */
    protected RedactionBuffer ipAddressElementBuffer() {
        return RedactionBuffer.DEFAULT;
    }

    private int redactIpAddressElements(final Redactor redactor) {
        LOGGER.debug("\t\t- Redacting IP address elements");
        return ipAddressElements()
            .stream()
            .flatMap(rootSelector -> driver.findElements(rootSelector).stream())
            .filter(element -> TextSearcher.hasIpAddress(element.getText(), browserInteractionHelper.getValue(element)))
            .mapToInt(element -> {
                redactor.redactIpAddress(element, ipAddressElementBuffer());
                return 1;
            })
            .sum();
    }

    /**
     * A {@link Collection} of {@link By} selectors for the user's visible passkey on the profile page.
     *
     * @return {@link By} selectors for passkey HTML elements
     */
    protected Collection<By> passkeyElements() {
        return List.of();
    }

    /**
     * The {@link RedactionBuffer} applied when redacting passkey elements. Override to adjust the buffer for a specific tracker.
     *
     * @return the {@link RedactionBuffer} for passkey redaction
     */
    protected RedactionBuffer passkeyElementBuffer() {
        return RedactionBuffer.DEFAULT;
    }

    private int redactPasskeyElements(final Redactor redactor) {
        LOGGER.debug("\t\t- Redacting passkey elements");
        return passkeyElements()
            .stream()
            .flatMap(rootSelector -> driver.findElements(rootSelector).stream())
            .mapToInt(element -> {
                redactor.redactPasskey(element, passkeyElementBuffer());
                return 1;
            })
            .sum();
    }

    /**
     * A {@link Map} of {@link By} selectors for any miscellaneous sensitive elements visible on the profile page, keyed by a description.
     *
     * @return {@link By} selectors for sensitive HTML elements, with a {@link String} describing the content
     */
    protected Map<String, By> sensitiveElements() {
        return Map.of();
    }

    /**
     * The {@link RedactionBuffer} applied when redacting sensitive elements. Override to adjust the buffer for a specific tracker.
     *
     * @return the {@link RedactionBuffer} for sensitive element redaction
     */
    protected RedactionBuffer sensitiveElementBuffer() {
        return RedactionBuffer.DEFAULT;
    }

    private int redactSensitiveElements(final Redactor redactor) {
        LOGGER.debug("\t\t- Redacting remaining sensitive elements");
        return sensitiveElements().entrySet()
            .stream()
            .mapToInt(entry -> {
                final String description = entry.getKey();
                final By selector = entry.getValue();
                driver.findElements(selector).forEach(element -> redactor.redact(element, description, sensitiveElementBuffer()));
                return 1;
            })
            .sum();
    }

    /**
     * Any action to be taken by the {@link AbstractTrackerHandler} while on the profile page, prior to taking a screenshot.
     *
     * <p>
     * By default, it hides the scrollbar without changing page dimensions, preventing layout shifts that would affect redaction positioning.
     *
     * @see BrowserInteractionHelper#hideScrollbar()
     */
    public void actionBeforeScreenshot() {
        browserInteractionHelper.hideScrollbar();
    }

    /**
     * Any action to be taken by the {@link AbstractTrackerHandler} while on the profile page, after taking a screenshot.
     *
     * <p>
     * By default, it re-enables scrolling and scrolls back to the top of the page.
     *
     * @see BrowserInteractionHelper#scrollToTheTop()
     * @see BrowserInteractionHelper#showScrollbar()
     */
    public void actionAfterScreenshot() {
        browserInteractionHelper.showScrollbar();
        browserInteractionHelper.scrollToTheTop();
    }

    /**
     * Retrieves the {@link WebElement} of the logout button.
     *
     * @return the logout button {@link WebElement}
     */
    protected abstract By logoutButtonSelector();

    /**
     * Logs out of the tracker, ending the user's session. Waits {@link #pageLoadDuration()} for the {@link #postLogoutElementSelector()} to
     * load, signifying that we have successfully logged out and been redirected to the login page.
     */
    public final void logout() {
        LOGGER.debug("\t- Logging out of tracker");
        final By logoutButtonSelector = logoutButtonSelector();
        browserInteractionHelper.waitForElementToAppear(logoutButtonSelector, pageLoadDuration());
        final WebElement logoutButton = browserInteractionHelper.waitForElementToBeInteractable(logoutButtonSelector, pageTransitionsDuration());
        clickButton(logoutButton);

        additionalActionAfterLogoutClick();
        browserInteractionHelper.waitForElementToAppear(postLogoutElementSelector(), pageTransitionsDuration());
    }

    /**
     * For certain trackers, additional actions may need to be performed after clicking on the logout button. For example, some trackers will have a
     * pop-up to confirm logout.
     *
     * <p>
     * This method should be overridden as required.
     */
    protected void additionalActionAfterLogoutClick() {
        // Do nothing by default
    }

    /**
     * Defines the {@link By} selectors of the {@link WebElement} that signifies that the {@link #logout()} was successfully executed.
     *
     * <p>
     * By default, we assume that we will be redirected to the login page, so this method returns {@link #usernameFieldSelector()}, or else the home
     * page, and {@link #loginPageSelector()} will be returned. Should be overridden otherwise.
     *
     * @return the post-logout button {@link WebElement}
     */
    protected By postLogoutElementSelector() {
        return loginPageSelector() == null ? usernameFieldSelector() : loginPageSelector();
    }

    @Override
    public void close() {
        LOGGER.trace("Closing driver");
        driver.quit();
    }

    /**
     * Retrieves the {@link RemoteWebDriver}.
     *
     * @return the {@link RemoteWebDriver}
     */
    public RemoteWebDriver driver() {
        return driver;
    }

    /**
     * Sometimes when clicking the login button or the profile page button, the page won't load correctly. If a {@link TimeoutException} occurs due to
     * the web page, not loading within {@code clickResolutionDuration} it is simply ignored. We then force the web page to stop loading before
     * proceeding.
     *
     * @param buttonToClick the {@link WebElement} to {@link WebElement#click()}
     * @see BrowserInteractionHelper#stopPageLoad()
     */
    protected void clickButton(final WebElement buttonToClick) {
        try {
            LOGGER.trace("Clicking: {}", buttonToClick);
            driver.manage().timeouts().pageLoadTimeout(maximumClickResolutionDuration());
            buttonToClick.click();
        } catch (final TimeoutException e) {
            LOGGER.debug("Page still loading after {}, force stopping page load", maximumClickResolutionDuration());
            LOGGER.trace(e);
            browserInteractionHelper.stopPageLoad();
        } catch (final Exception e) {
            LOGGER.trace(driver.getPageSource());
            LOGGER.trace(e);
            throw e;
        }

        driver.manage().timeouts().pageLoadTimeout(maximumLinkResolutionDuration());
        BrowserInteractionHelper.explicitWait(pageTransitionsDuration(), "button click");
    }

    private RemoteWebDriver createRemoteWebDriver(final TrackerType trackerType, final List<Extension> requiredExtensions) {
        if (trackerType == TrackerType.CLOUDFLARE_CHECK) {
            if (!requiredExtensions.isEmpty()) {
                LOGGER.trace("Attempting to create python driver with extensions; extensions will be installed but cannot be configured: {}",
                    requiredExtensions);
            }
            return PythonWebDriverFactory.createDriver(requiredExtensions);
        }

        final boolean needsExplicitTranslation = this instanceof NeedsExplicitTranslation;
        final RemoteWebDriver configurationDriver = JavaWebDriverFactory.createDriver(trackerType, needsExplicitTranslation, requiredExtensions);

        for (final Extension extension : requiredExtensions) {
            extension.configure(configurationDriver);
        }

        return configurationDriver;
    }
}
