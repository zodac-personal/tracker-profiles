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
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.zodac.tracker.app.ScreenshotOrchestrator;
import net.zodac.tracker.framework.TrackerDefinition;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.driver.extension.Extension;
import net.zodac.tracker.framework.driver.extension.UblockOriginLiteExtension;
import net.zodac.tracker.framework.driver.java.JavaWebDriverFactory;
import net.zodac.tracker.framework.driver.python.PythonWebDriverFactory;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.redaction.Redactor;
import net.zodac.tracker.redaction.RedactorImpl;
import net.zodac.tracker.util.ScriptExecutor;
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
 * Abstract class used to define a {@link AbstractTrackerHandler}. All implementations will be used by {@link ScreenshotOrchestrator},
 * if the tracker is included in the tracker input file. This class lists the high-level methods required for {@link ScreenshotOrchestrator} to be
 * able to successfully generate a screenshot for a given tracker.
 *
 * <p>
 * Since each tracker website has its own UI and own page structure, each implementation of {@link AbstractTrackerHandler} will contain the
 * tracker-specific {@code selenium} logic to perform the UI actions.
 */
public abstract class AbstractTrackerHandler implements AutoCloseable {

    /**
     * The default {@link By} selector for the Cloudflare element to be clicked, if a tracker has a Cloudflare verification check.
     */
    protected static final By DEFAULT_CLOUDFLARE_SELECTOR = XpathBuilder
        .from(div, withClass("main-content"))
        .descendant(div, atIndex(2))
        .build();

    /**
     * The logger instance.
     */
    protected static final Logger LOGGER = LogManager.getLogger();

    private static final Duration DEFAULT_MAXIMUM_CLICK_RESOLUTION_DURATION = Duration.ofSeconds(15L);
    private static final Duration DEFAULT_MAXIMUM_LINK_RESOLUTION_TIME = Duration.ofMinutes(2L);
    private static final Duration DEFAULT_WAIT_FOR_PAGE_LOAD_DURATION = Duration.ofSeconds(5L);
    private static final Duration DEFAULT_WAIT_FOR_PAGE_UPDATES_DURATION = Duration.ofSeconds(1L);
    private static final Duration DEFAULT_WAIT_FOR_TRANSITIONS_DURATION = Duration.ofMillis(500L);

    /**
     * The {@link RemoteWebDriver} instance used to load web pages and perform UI actions.
     */
    @SuppressWarnings("NullAway") // Will be set in the configure() method
    protected RemoteWebDriver driver;

    /**
     * The {@link ScriptExecutor} instance to perform specific actions for each {@link AbstractTrackerHandler} implementation.
     */
    @SuppressWarnings("NullAway") // Will be set in the configure() method
    protected ScriptExecutor scriptExecutor;

    @SuppressWarnings("NullAway") // Will be set in the configure() method
    private TrackerDefinition trackerDefinition;

    /**
     * The {@link Redactor} instance to redact sensitive information on user profile pages.
     */
    @SuppressWarnings("NullAway") // Will be set in the configure() method
    protected Redactor redactor;

    /**
     * We use a no-arg constructor to instantiate the {@link AbstractTrackerHandler} to avoid needing to define a constructor for each implementation.
     * However, we still need to configure the {@link AbstractTrackerHandler} with details for the tracker for execution, so we overwrite the default
     * values that were already set.
     *
     * @param trackerDefinition the {@link TrackerDefinition} for this {@link AbstractTrackerHandler}
     */
    public void configure(final TrackerDefinition trackerDefinition) {
        this.trackerDefinition = trackerDefinition;
        driver = createRemoteWebDriver(trackerDefinition.type());
        scriptExecutor = new ScriptExecutor(driver);
        redactor = new RedactorImpl(driver);

        if (installAdBlocker()) {
            final Extension adBlockerExtension = new UblockOriginLiteExtension();
            adBlockerExtension.configure(driver, scriptExecutor);
        }
    }

    /**
     * Navigates to the home page of the tracker. Waits {@link #waitForPageLoadDuration()} for the page to finish loading.
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
                    scriptExecutor.waitForPageToLoad(waitForPageLoadDuration());
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
            scriptExecutor.waitForElementToAppear(usernameFieldSelector(), waitForPageLoadDuration());
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
        final By cloudflareSelector = cloudflareSelector();
        if (cloudflareSelector == null) {
            return;
        }

        LOGGER.debug("\t- Performing Cloudflare verification check");
        scriptExecutor.waitForPageToLoad(waitForPageLoadDuration());
        LOGGER.info("\t\t >>> Waiting for user to pass the Cloudflare verification");

        final WebElement cloudflareElement = driver.findElement(cloudflareSelector);
        // TODO Use cloudflareElement.getShadowRoot()?
        scriptExecutor.highlightElement(cloudflareElement);
        DisplayUtils.userInputConfirmation(trackerName, "Pass the Cloudflare verification");
    }

    /**
     * If the tracker has a Cloudflare verification check, returns the {@link By} selector for the Cloudflare element.
     *
     * <p>
     * By default, we assume there is no Cloudflare check, so this method returns {@code null}, but should be overridden otherwise. The common
     * implementation to bypass the check is performed by {@link #cloudflareCheck(String)}.
     *
     * @return the {@link By} selector if there is a Cloudflare check
     */
    @Nullable
    protected By cloudflareSelector() {
        return null;
    }

    /**
     * Enters the user's credential and logs in to the tracker. Waits {@link #waitForPageLoadDuration()} for the page to finish loading.
     *
     * @param username    the user's username for the tracker
     * @param password    the user's password for the tracker
     * @param trackerName the name of the tracker
     */
    public void login(final String username, final String password, final String trackerName) {
        LOGGER.trace("Logging in to tracker '{}'", trackerName);
        scriptExecutor.waitForPageToLoad(waitForPageUpdateDuration());
        LOGGER.trace("Entering username");
        final WebElement usernameField = driver.findElement(usernameFieldSelector());
        usernameField.clear();
        usernameField.sendKeys(username);

        LOGGER.trace("Entering password");
        final WebElement passwordField = driver.findElement(passwordFieldSelector());
        passwordField.clear();
        passwordField.sendKeys(password);

        manualCheckBeforeLoginClick(trackerName);

        // TODO: Check if the web page has changed (user clicked login during manual operation), and skip this?
        final By loginButtonSelector = loginButtonSelector();
        if (loginButtonSelector != null) {
            final WebElement loginButton = driver.findElement(loginButtonSelector);
            LOGGER.trace("Clicking login button: {}", loginButton);
            clickButton(loginButton);
        }
        manualCheckAfterLoginClick(trackerName);

        final By postLoginSelector = postLoginSelector();
        LOGGER.trace("Logged in, waiting for post login selector: {}", postLoginSelector);
        ScriptExecutor.explicitWait(waitForPageUpdateDuration(), "page to load after login");
        scriptExecutor.waitForElementToAppear(postLoginSelector, waitForPageLoadDuration());
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
     * @param trackerName the name of the tracker
     * @see ScriptExecutor#highlightElement(WebElement)
     */
    protected void manualCheckBeforeLoginClick(final String trackerName) {
        // Do nothing by default
    }

    /**
     * Defines the {@link By} selector of the {@link WebElement} of the login button. Can be {@link Nullable} if there is no login button, and a user
     * interaction is required instead,
     *
     * @return the login button {@link By} selector
     */
    @Nullable
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
     * @param trackerName the name of the tracker
     * @see ScriptExecutor#highlightElement(WebElement)
     */
    protected void manualCheckAfterLoginClick(final String trackerName) {
        // Do nothing by default
    }

    /**
     * Defines the {@link By} selector of the {@link WebElement} used to confirm that the user has successfully logged in.
     *
     * @return the post-login {@link By} selector
     */
    protected abstract By postLoginSelector();
    // TODO: Shouldn't this usually be the profile selector?

    /**
     * Checks if there is a banner on the tracker web page, and closes it. This may be a cookie banner, or some other warning banner that can
     * obscure content, or expose unwanted information.
     *
     * <p>
     * By default, we assume there is no banner to clear, so this method returns {@code false}. Should be overridden otherwise.
     *
     * @return {@code true} if there was a banner, and it was cleared
     */
    public boolean canBannerBeCleared() {
        return false;
    }

    /**
     * Once logged in, navigates to the user's profile page on the tracker. Waits {@link #waitForPageLoadDuration()} for the page to finish
     * loading.
     */
    // TODO: Add a postProfilePageSelector() to confirm the user profile has been loaded?
    public void openProfilePage() {
        LOGGER.trace("Opening profile page");
        scriptExecutor.waitForPageToLoad(waitForPageUpdateDuration());

        final WebElement profilePageLink = driver.findElement(profilePageSelector());
        scriptExecutor.removeAttribute(profilePageLink, "target"); // Removing 'target="_blank"', to ensure link opens in same tab
        clickButton(profilePageLink);

        // TODO: Move this to TRY block of clickButton()? Test against RuTracker
        scriptExecutor.waitForPageToLoad(waitForPageLoadDuration());
        scriptExecutor.moveToOrigin();
        additionalActionOnProfilePage();
    }

    /**
     * Defines the {@link By} selector of the {@link WebElement} of the user's profile page.
     *
     * @return the profile page {@link By} selector
     */
    protected By profilePageSelector() {
        return XpathBuilder
            .from(a, withClass("username"))
            .build();
    }

    /**
     * For certain trackers, additional actions may need to be performed after opening the profile page, but prior to the page being redacted and
     * screenshot. This might be that the page is considered 'loaded' by
     * {@link ScriptExecutor#waitForPageToLoad(Duration)}, but the required {@link WebElement}s are not all
     * on the screen, or that some {@link WebElement}s may need to be interacted with prior to the screenshot.
     *
     * <p>
     * This method should be overridden as required.
     */
    protected void additionalActionOnProfilePage() {
        // Do nothing by default
    }

    /**
     * Checks if the {@link AbstractTrackerHandler} has any sensitive information that requires redaction of any elements.
     *
     * @return <code>true</code> if there are elements in need of redaction
     */
    public boolean hasSensitiveInformation() {
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
     * @return the number of {@link WebElement}s where the text has been redacted
     * @see Redactor
     */
    public int redactElements() {
        LOGGER.trace("Redacting elements");
        return redactEmailElements()
            + redactIpAddressElements()
            + redactPasskeyElements()
            + redactSensitiveElements();
    }

    /**
     * A {@link Collection} of {@link By} selectors for the user's visible email address on the profile page.
     *
     * @return {@link By} selectors for email HTML elements
     */
    protected Collection<By> emailElements() {
        return List.of();
    }

    private int redactEmailElements() {
        LOGGER.debug("\t\t- Redacting email elements");
        final Collection<WebElement> emailElements = emailElements()
            .stream()
            .flatMap(rootSelector -> driver.findElements(rootSelector).stream())
            .filter(element -> TextSearcher.containsEmailAddress(element.getText()))
            .toList();

        for (final WebElement element : emailElements) {
            redactor.redactEmail(element);
        }

        return emailElements.size();
    }

    /**
     * A {@link Collection} of {@link By} selectors for the user's visible IP address on the profile page.
     *
     * @return {@link By} selectors for IP address HTML elements
     */
    protected Collection<By> ipAddressElements() {
        return List.of();
    }

    private int redactIpAddressElements() {
        LOGGER.debug("\t\t- Redacting IP address elements");
        final Collection<WebElement> ipAddressElements = ipAddressElements()
            .stream()
            .flatMap(rootSelector -> driver.findElements(rootSelector).stream())
            .filter(element -> TextSearcher.containsIpAddress(element.getText()))
            .toList();

        for (final WebElement element : ipAddressElements) {
            redactor.redactIpAddress(element);
        }

        return ipAddressElements.size();
    }

    /**
     * A {@link Collection} of {@link By} selectors for the user's visible passkey on the profile page.
     *
     * @return {@link By} selectors for passkey HTML elements
     */
    protected Collection<By> passkeyElements() {
        return List.of();
    }

    private int redactPasskeyElements() {
        LOGGER.debug("\t\t- Redacting passkey elements");
        final Collection<WebElement> passkeyElements = passkeyElements()
            .stream()
            .flatMap(rootSelector -> driver.findElements(rootSelector).stream())
            .toList();

        for (final WebElement element : passkeyElements) {
            redactor.redactPasskey(element);
        }

        return passkeyElements.size();
    }

    /**
     * A {@link Map} of {@link By} selectors for any miscellaneous sensitive elements visible on the profile page, keyed by a description.
     *
     * @return {@link By} selectors for sensitive HTML elements, with a {@link String} describing the content
     */
    protected Map<String, By> sensitiveElements() {
        return Map.of();
    }

    private int redactSensitiveElements() {
        LOGGER.debug("\t\t- Redacting remaining sensitive elements");
        for (final Map.Entry<String, By> entry : sensitiveElements().entrySet()) {
            final String description = entry.getKey();
            final By selector = entry.getValue();

            for (final WebElement element : driver.findElements(selector)) {
                redactor.redact(element, description);
            }
        }

        return sensitiveElements().size();
    }

    /**
     * Reload the current page (should be the user profile page) to clear any redactions.
     */
    public void reloadPage() {
        // Reload page to clear redacted elements
        driver.navigate().refresh();
        scriptExecutor.waitForPageToLoad(waitForPageLoadDuration());
    }

    /**
     * Checks if there is a header on the tracker's user profile, and updates it to not be fixed. This is to avoid the banner appearing multiple times
     * in the user profile screenshot as we scroll through the page.
     *
     * <p>
     * By default, we assume there is no header to update, so this method returns {@code false}. Should be overridden otherwise.
     *
     * @return {@code true} if there was a fixed header, and it was successfully updated
     */
    public boolean hasFixedHeader() {
        return false;
    }

    /**
     * Retrieves the {@link WebElement} of the logout button.
     *
     * @return the logout button {@link WebElement}
     */
    protected abstract By logoutButtonSelector();

    /**
     * Logs out of the tracker, ending the user's session. Waits {@link #waitForPageLoadDuration()} for the {@link #postLogoutElementSelector()} to
     * load, signifying that we have successfully logged out and been redirected to the login page.
     */
    public void logout() {
        LOGGER.debug("\t\t- Logging out of tracker");
        final By logoutButtonSelector = logoutButtonSelector();
        scriptExecutor.waitForElementToAppear(logoutButtonSelector, waitForPageLoadDuration());
        final WebElement logoutButton = driver.findElement(logoutButtonSelector);
        clickButton(logoutButton);

        scriptExecutor.waitForPageToLoad(waitForPageLoadDuration());
        scriptExecutor.waitForElementToAppear(postLogoutElementSelector(), waitForTransitionsDuration());
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
     * @see ScriptExecutor#stopPageLoad()
     */
    protected void clickButton(final WebElement buttonToClick) {
        try {
            LOGGER.trace("Clicking: {}", buttonToClick);
            driver.manage().timeouts().pageLoadTimeout(maximumClickResolutionDuration());
            buttonToClick.click();
        } catch (final TimeoutException e) {
            LOGGER.debug("Page still loading after {}, force stopping page load", maximumClickResolutionDuration());
            LOGGER.trace(e);
            scriptExecutor.stopPageLoad();
        } catch (final Exception e) {
            LOGGER.trace(driver.getPageSource());
            LOGGER.trace(e);
            throw e;
        }

        driver.manage().timeouts().pageLoadTimeout(DEFAULT_MAXIMUM_LINK_RESOLUTION_TIME);
        ScriptExecutor.explicitWait(waitForTransitionsDuration(), "button click");
    }

    // TODO: Move timers to an interface out of this class?

    /**
     * The maximum {@link Duration} for a click to complete its action.
     *
     * @return the maximum click resolution {@link Duration}
     */
    protected Duration maximumClickResolutionDuration() {
        return DEFAULT_MAXIMUM_CLICK_RESOLUTION_DURATION;
    }

    /**
     * The maximum {@link Duration} for a link to complete loading.
     *
     * @return the maximum load resolution {@link Duration}
     */
    protected Duration maximumLinkResolutionDuration() {
        return DEFAULT_MAXIMUM_LINK_RESOLUTION_TIME;
    }

    /**
     * The {@link Duration} to wait for a page load.
     *
     * @return the page load wait {@link Duration}
     */
    protected Duration waitForPageLoadDuration() {
        return DEFAULT_WAIT_FOR_PAGE_LOAD_DURATION;
    }

    /**
     * The {@link Duration} to wait for a page/element transition.
     *
     * @return the transition wait {@link Duration}
     */
    protected Duration waitForTransitionsDuration() {
        return DEFAULT_WAIT_FOR_TRANSITIONS_DURATION;
    }

    /**
     * The {@link Duration} to wait for a page updates.
     *
     * @return the page updates wait {@link Duration}
     */
    // TODO: Use waitForTransitionsDuration() instead?
    protected Duration waitForPageUpdateDuration() {
        return DEFAULT_WAIT_FOR_PAGE_UPDATES_DURATION;
    }

    /**
     * Whether the {@link AbstractTrackerHandler} requires an ad-blocker to be installed. By default this is {@code false} to avoid needing to
     * configure the ad-blocker.
     *
     * @return whether to install an ad-blocker for the {@link AbstractTrackerHandler}
     */
    protected boolean installAdBlocker() {
        return false;
    }

    private RemoteWebDriver createRemoteWebDriver(final TrackerType trackerType) {
        final boolean installAdBlocker = installAdBlocker();
        return trackerType == TrackerType.CLOUDFLARE_CHECK
            ? PythonWebDriverFactory.createDriver(installAdBlocker)
            : JavaWebDriverFactory.createDriver(trackerType, installAdBlocker);
    }
}
