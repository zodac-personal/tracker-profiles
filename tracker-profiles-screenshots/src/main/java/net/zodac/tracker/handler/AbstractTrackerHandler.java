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
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import net.zodac.tracker.ProfileScreenshotter;
import net.zodac.tracker.framework.TrackerDefinition;
import net.zodac.tracker.framework.TrackerType;
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
 * Abstract class used to define a {@link AbstractTrackerHandler}. All implementations will be used by {@link ProfileScreenshotter},
 * if the tracker is included in the tracker input file. This class lists the high-level methods required for {@link ProfileScreenshotter} to be able
 * to successfully generate a screenshot for a given tracker.
 *
 * <p>
 * Since each tracker website has its own UI and own page structure, each implementation of {@link AbstractTrackerHandler} will contain the
 * tracker-specific {@code selenium} logic to perform the UI actions.
 */
// TODO: Can we add a private no-args constructor and make this final?
public abstract class AbstractTrackerHandler implements AutoCloseable {

    /**
     * The default {@link By} selector for the Cloudflare element to be clicked, if a tracker has a Cloudflare verification check.
     */
    protected static final By DEFAULT_CLOUDFLARE_SELECTOR = XpathBuilder
        .from(div, withClass("main-content"))
        .descendant(div, atIndex(2))
        .build();

    /**
     * The default wait {@link Duration} when waiting for a web page load.
     */
    protected static final Duration DEFAULT_WAIT_FOR_PAGE_LOAD = Duration.of(5L, ChronoUnit.SECONDS);

    /**
     * The default wait {@link Duration} when waiting for an element to be clicked or a page load to begin.
     */
    protected static final Duration DEFAULT_WAIT_FOR_TRANSITIONS = Duration.of(500L, ChronoUnit.MILLIS);

    /**
     * The standard wait {@link Duration} to let the login page load.
     */
    protected static final Duration WAIT_FOR_LOGIN_PAGE_LOAD = Duration.of(1L, ChronoUnit.SECONDS);

    /**
     * The logger instance.
     */
    protected static final Logger LOGGER = LogManager.getLogger();

    private static final Duration MAXIMUM_LINK_RESOLUTION_TIME = Duration.of(2L, ChronoUnit.MINUTES);
    private static final Duration MAXIMUM_CLICK_RESOLUTION_TIME = Duration.of(15L, ChronoUnit.SECONDS);

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
    }

    /**
     * Navigates to the home page of the tracker. Waits {@link #DEFAULT_WAIT_FOR_PAGE_LOAD} for the page to finish loading.
     */
    public void openTracker() {
        boolean unableToConnect = true;
        for (final String trackerUrl : trackerDefinition.urls()) {
            try {
                LOGGER.info("\t\t- '{}'", trackerUrl);
                driver.manage().timeouts().pageLoadTimeout(MAXIMUM_LINK_RESOLUTION_TIME);
                driver.navigate().to(trackerUrl);

                // Explicit check for Cloudflare Error 523 if a site is unavailable
                final String bodyText = driver.findElement(By.tagName("body")).getText();
                if (bodyText.contains("HTTP ERROR 523") || bodyText.contains("Error code 523")) {
                    LOGGER.warn("\t\t- Unable to connect: Cloudflare Error 523 (Origin is unreachable)");
                    continue;
                }

                unableToConnect = false;
                scriptExecutor.waitForPageToLoad(DEFAULT_WAIT_FOR_PAGE_LOAD);
                break; // No need to load another page
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
        if (unableToConnect) {
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
        final By loginLinkSelector = loginPageSelector();

        if (loginLinkSelector != null) {
            final WebElement loginLink = driver.findElement(loginLinkSelector);
            clickButton(loginLink);
            cloudflareCheck(trackerName);
            scriptExecutor.waitForElementToAppear(usernameFieldSelector(), DEFAULT_WAIT_FOR_PAGE_LOAD);
        } else {
            cloudflareCheck(trackerName);
        }
    }

    /**
     * For this {@link AbstractTrackerHandler} implementation, there is a Cloudflare check protecting the login page. This verification check must be
     * passed to proceed. This must be done within {@link DisplayUtils#INPUT_WAIT_DURATION}.
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

        scriptExecutor.waitForPageToLoad(DEFAULT_WAIT_FOR_PAGE_LOAD);
        LOGGER.info("\t\t >>> Waiting for user to pass the Cloudflare verification, for {} seconds",
            DisplayUtils.INPUT_WAIT_DURATION.getSeconds());

        final WebElement cloudflareElement = driver.findElement(cloudflareSelector);
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
     * Enters the user's credential and logs in to the tracker. Waits {@link #DEFAULT_WAIT_FOR_PAGE_LOAD} for the page to finish loading.
     *
     * @param username    the user's username for the tracker
     * @param password    the user's password for the tracker
     * @param trackerName the name of the tracker
     */
    public void login(final String username, final String password, final String trackerName) {
        ScriptExecutor.explicitWait(WAIT_FOR_LOGIN_PAGE_LOAD);
        LOGGER.trace("Entering username");
        final WebElement usernameField = driver.findElement(usernameFieldSelector());
        usernameField.clear();
        usernameField.sendKeys(username);

        LOGGER.trace("Entering password");
        final WebElement passwordField = driver.findElement(passwordFieldSelector());
        passwordField.clear();
        passwordField.sendKeys(password);

        manualCheckBeforeLoginClick(trackerName);

        // TODO: Check if the webpage has changed (user clicked login during manual operation), and skip this
        final By loginButtonSelector = loginButtonSelector();
        if (loginButtonSelector != null) {
            final WebElement loginButton = driver.findElement(loginButtonSelector);
            LOGGER.trace("Clicking login button: {}", loginButton);
            clickButton(loginButton);
        }
        manualCheckAfterLoginClick(trackerName);

        ScriptExecutor.explicitWait(WAIT_FOR_LOGIN_PAGE_LOAD);
        scriptExecutor.waitForElementToAppear(postLoginSelector(), DEFAULT_WAIT_FOR_PAGE_LOAD);
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
     * Once logged in, navigates to the user's profile page on the tracker. Waits {@link #DEFAULT_WAIT_FOR_PAGE_LOAD} for the page to finish
     * loading.
     */
    public void openProfilePage() {
        ScriptExecutor.explicitWait(WAIT_FOR_LOGIN_PAGE_LOAD);

        final WebElement profilePageLink = driver.findElement(profilePageSelector());
        scriptExecutor.removeAttribute(profilePageLink, "target"); // Removing 'target="_blank"', to ensure link opens in same tab
        clickButton(profilePageLink);

        // TODO: Move this to TRY block of clickButton()? Test against RuTracker
        scriptExecutor.waitForPageToLoad(DEFAULT_WAIT_FOR_PAGE_LOAD);
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
     * Checks if the {@link AbstractTrackerHandler} requires redaction of any elements.
     *
     * <p>
     * By default, it will check the result of {@link #getElementsPotentiallyContainingSensitiveInformation()}, and only return <code>true</code> if
     * it is not {@link Collection#isEmpty()}.
     *
     * @return <code>true</code> if there are elements in need of redaction
     */
    public boolean hasElementsNeedingRedaction() {
        return !getElementsPotentiallyContainingSensitiveInformation().isEmpty();
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
        final Collection<By> selectors = getElementsPotentiallyContainingSensitiveInformation();
        if (selectors.isEmpty()) {
            LOGGER.trace("\t\t- No defined elements to redact");
            return 0;
        }

        // TODO: Test all trackers when not connectable/no torrents being seeded, and ensure screenshots work as expected
        // TODO: Define which elements to redact?
        final Collection<WebElement> emailElements = selectors
            .stream()
            .flatMap(rootSelector -> driver.findElements(rootSelector).stream())
            .filter(element -> TextSearcher.containsEmailAddress(element.getText()))
            .toList();

        final Collection<WebElement> ipElements = selectors
            .stream()
            .flatMap(rootSelector -> driver.findElements(rootSelector).stream())
            .filter(element -> TextSearcher.containsIpAddress(element.getText()))
            .toList();

        if (emailElements.isEmpty() && ipElements.isEmpty()) {
            LOGGER.warn("\t\t- Unexpectedly found no elements to redact");
            return 0;
        }

        for (final WebElement element : emailElements) {
            redactor.redactEmail(element);
        }

        for (final WebElement element : ipElements) {
            redactor.redactIpAddress(element);
        }

        return emailElements.size() + ipElements.size();
    }

    /**
     * Returns a {@link Collection} of {@link By} selectors that define all possible HTML elements that may contain sensitive data to be redacted.
     *
     * <p>
     * By default, we assume that there are no elements to redact, so this method returns an empty {@link List}. Should be overridden
     * otherwise.
     *
     * @return the {@link By} selectors for elements that may contain sensitive information
     */
    protected Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of();
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
     * Checks if the web page is using a non-English language, and translates it to English.
     *
     * <p>
     * By default, we assume there is no header to update, so this method returns {@code false}. Should be overridden otherwise.
     *
     * @param username the username, to be re-applied to the web page in case of accidental translation
     * @return {@code true} if the site is not in English, and it was successfully translated
     */
    public boolean isNotEnglish(final String username) {
        return false;
    }

    /**
     * Retrieves the {@link WebElement} of the logout button.
     *
     * @return the logout button {@link WebElement}
     */
    protected abstract By logoutButtonSelector();

    /**
     * Logs out of the tracker, ending the user's session. Waits {@link #DEFAULT_WAIT_FOR_PAGE_LOAD} for the {@link #postLogoutElementSelector()} to
     * load, signifying that we have successfully logged out and been redirected to the login page.
     */
    public void logout() {
        final By logoutButtonSelector = logoutButtonSelector();
        scriptExecutor.waitForElementToAppear(logoutButtonSelector, DEFAULT_WAIT_FOR_PAGE_LOAD);
        final WebElement logoutButton = driver.findElement(logoutButtonSelector);
        clickButton(logoutButton);

        scriptExecutor.waitForPageToLoad(DEFAULT_WAIT_FOR_PAGE_LOAD);
        scriptExecutor.waitForElementToAppear(postLogoutElementSelector(), DEFAULT_WAIT_FOR_TRANSITIONS);
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
     * Same as {@link #clickButton(WebElement, Duration)}, but the click resolution time is {@link #MAXIMUM_CLICK_RESOLUTION_TIME}.
     *
     * @param buttonToClick the {@link WebElement} to {@link WebElement#click()}
     * @see #clickButton(WebElement)
     * @see ScriptExecutor#stopPageLoad()
     */
    protected void clickButton(final WebElement buttonToClick) {
        clickButton(buttonToClick, MAXIMUM_CLICK_RESOLUTION_TIME);
    }

    /**
     * Sometimes when clicking the login button or the profile page button, the page won't load correctly. If a {@link TimeoutException} occurs due to
     * the webpage, not loading within {@code clickResolutionDuration} it is simply ignored. We then force the web page to stop loading before
     * proceeding.
     *
     * @param buttonToClick           the {@link WebElement} to {@link WebElement#click()}
     * @param clickResolutionDuration the {@link Duration} to wait for the click to action a change (like a page load)
     * @see ScriptExecutor#stopPageLoad()
     */
    protected void clickButton(final WebElement buttonToClick, final Duration clickResolutionDuration) {
        try {
            driver.manage().timeouts().pageLoadTimeout(clickResolutionDuration);
            buttonToClick.click();
        } catch (final TimeoutException e) {
            LOGGER.debug("Page still loading after {}, force stopping page load", clickResolutionDuration);
            LOGGER.trace(e);
            scriptExecutor.stopPageLoad();
        } catch (final Exception e) {
            LOGGER.trace(driver.getPageSource());
            LOGGER.trace(e);
            throw e;
        }

        driver.manage().timeouts().pageLoadTimeout(MAXIMUM_LINK_RESOLUTION_TIME);
        ScriptExecutor.explicitWait(DEFAULT_WAIT_FOR_TRANSITIONS);
    }

    private static RemoteWebDriver createRemoteWebDriver(final TrackerType trackerType) {
        return trackerType == TrackerType.CLOUDFLARE_CHECK ? PythonWebDriverFactory.createDriver() : JavaWebDriverFactory.createDriver(trackerType);
    }
}
