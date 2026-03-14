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

import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.handler.definition.NeedsExplicitTranslation;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Extension of the {@link TsSpecialEditionHandler} for the {@code LetSeed} tracker.
 */
@TrackerHandler(name = "LetSeed", type = TrackerType.MANUAL, url = "https://letseed.org/")
public class LetSeed extends TsSpecialEditionHandler implements NeedsExplicitTranslation {

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link LetSeed}, prior to clicking the login button with a successful username/password there is another field where a
     * Captcha needs to be entered.
     *
     * <p>
     * Manual user interaction:
     * <ol>
     *     <li>Enter correct captcha value</li>
     * </ol>
     */
    @Override
    protected void manualCheckBeforeLoginClick() {
        LOGGER.info("\t\t >>> Waiting for user to enter captcha");

        final By captchaSelector = By.id("regimage");
        final WebElement captchaElement = driver.findElement(captchaSelector);
        browserInteractionHelper.highlightElement(captchaElement);
        DisplayUtils.userInputConfirmation(trackerDefinition.name(), "Solve the captcha");
    }

    @Override
    public void translatePageToEnglish() {
        final By englishLanguageOptionSelector = XpathBuilder
            .from(div, withClass("flags"))
            .child(a, atIndex(1))
            .build();
        final WebElement englishLanguageOption = driver.findElement(englishLanguageOptionSelector);
        clickButton(englishLanguageOption);
    }

    @Override
    protected By logoutButtonSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withClass("qactions"))
            .child(a, atIndex(2))
            .build();
    }

    private void openUserDropdownMenu() {
        // Click the user dropdown menu bar to make the profile/logout button interactable
        final By logoutParentSelector = By.id("quickprofileview");
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        clickButton(logoutParent);
    }
}
