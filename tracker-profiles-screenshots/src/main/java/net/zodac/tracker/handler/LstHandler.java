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

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Extension of the {@link Unit3dHandler} for the {@code LST} tracker.
 */
@TrackerHandler(name = "LST", type = TrackerType.MANUAL, url = "https://lst.gg/")
public class LstHandler extends Unit3dHandler {

    /**
     * Default constructor.
     *
     * @param driver      a {@link RemoteWebDriver} used to load web pages and perform UI actions
     * @param trackerUrls the URLs to the tracker
     */
    public LstHandler(final RemoteWebDriver driver, final Collection<String> trackerUrls) {
        super(driver, trackerUrls);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link LstHandler}, after clicking the login button with a successful username/password, another section pops up. There is a
     * multiple-choice question, where the correct movie poster must be chosen that matches the title, and the login button pressed again.
     * This must be done within {@link DisplayUtils#INPUT_WAIT_DURATION}.
     *
     * <p>
     * Manual user interactions:
     * <ol>
     *     <li>Select correct answer to question</li>
     *     <li>Click the login button</li>
     * </ol>
     */
    @Override
    protected void manualCheckAfterLoginClick(final String trackerName) {
        final String initialUrl = driver.getCurrentUrl();
        LOGGER.info("\t\t >>> Waiting for user to select correct movie poster and click the login button, for {} seconds",
            DisplayUtils.INPUT_WAIT_DURATION.getSeconds());

        final WebElement selectionElement = driver.findElement(By.xpath("//div[contains(@class, 'auth-form__text-input-group')]"));
        scriptExecutor.highlightElement(selectionElement);
        DisplayUtils.userInputConfirmation(trackerName, "Select the correct movie and click the login button");

        // If the user didn't click 'login', do it for them
        final String nextUrl = driver.getCurrentUrl();
        if (nextUrl == null || nextUrl.equalsIgnoreCase(initialUrl)) {
            final By loginButtonSelector = loginButtonSelector();
            if (loginButtonSelector != null) {
                final WebElement loginButton = driver.findElement(loginButtonSelector);
                clickButton(loginButton);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link LstHandler}, unlike most other {@code UNIT3D}-based trackers, there is no link to the username available in the nav bar. Instead, we
     * must move the mouse over the profile icon in the nav bar. This will activate the dropdown menu and make the profile link button interactive.
     *
     * @return {@link By} the selector for the profile link
     */
    @Override
    protected By profilePageSelector() {
        final By profileParentSelector = By.xpath("//div[contains(@class, 'top-nav__right')]//li[contains(@class, 'top-nav__dropdown')]");
        final WebElement profileParent = driver.findElement(profileParentSelector);
        scriptExecutor.moveTo(profileParent);

        return By.xpath("//a[contains(@class, 'top-nav__username')]");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link LstHandler}, the email is actually defined in a separate section, unlike other {@code UNIT3D}-based trackers.
     *
     * @return the {@link By} selectors for email and IP addresses
     */
    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            By.xpath("//table[contains(@class, 'data-table')]/tbody/tr/td[2]"), // IP address, potentially multiple entries
            By.xpath("//span[contains(@class, 'profile-hero__account-value--email')]") // Email
        );
    }
}
