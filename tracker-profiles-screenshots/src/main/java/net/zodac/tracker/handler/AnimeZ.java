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
import static net.zodac.tracker.framework.xpath.HtmlElement.button;
import static net.zodac.tracker.framework.xpath.HtmlElement.div;
import static net.zodac.tracker.framework.xpath.HtmlElement.form;
import static net.zodac.tracker.framework.xpath.HtmlElement.img;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;
import static net.zodac.tracker.framework.xpath.XpathAxis.parent;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.handler.definition.HasFixedHeader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code AnimeZ} tracker.
 */
@TrackerHandler(name = "AnimeZ", type = TrackerType.MANUAL, url = "https://animez.to/")
public class AnimeZ extends AbstractTrackerHandler implements HasFixedHeader {

    @Override
    public By loginPageSelector() {
        return XpathBuilder
            .from(div, withClass("portal-actions"))
            .child(a, atIndex(1))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link AnimeZ}, prior to clicking the login button with a successful username/password there is another field where a
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

        final By captchaSelector = XpathBuilder
            .from(input, withName("captcha"))
            .navigateTo(parent(div))
            .child(div, atIndex(1))
            .child(img, atIndex(1))
            .build();
        final WebElement captchaElement = driver.findElement(captchaSelector);
        browserInteractionHelper.highlightElement(captchaElement);
        DisplayUtils.userInputConfirmation(trackerDefinition.name(), "Solve the captcha");
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(button, withType("submit"))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        return XpathBuilder
            .from(span, withClass("avatar"))
            .navigateTo(parent(a))
            .build();
    }

    @Override
    protected By profilePageSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withClass("navbar-nav"), withClass("order-md-last"))
            .child(div, atIndex(2))
            .child(div, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    protected Collection<By> emailElements() {
        return List.of(
            XpathBuilder
                .from(div, withClass("card"))
                .child(div, withClass("card-body"))
                .descendant(div, withClass("datagrid-content"))
                .build()
        );
    }

    @Override
    public void unfixHeader() {
        LOGGER.debug("\t\t- Unfixing header");
        final By headerSelector = By.tagName("header");
        final WebElement headerElement = driver.findElement(headerSelector);
        browserInteractionHelper.makeUnfixed(headerElement);
    }

    @Override
    protected By logoutButtonSelector() {
        openUserDropdownMenu();
        return XpathBuilder
            .from(div, withClass("navbar-nav"), withClass("order-md-last"))
            .child(div, atIndex(2))
            .child(div, atIndex(1))
            .child(form, atIndex(1))
            .child(button, atIndex(1))
            .build();
    }

    private void openUserDropdownMenu() {
        LOGGER.debug("\t\t- Clicking user dropdown menu to make profile/logout button interactable");
        final By logoutParentSelector = XpathBuilder
            .from(span, withClass("avatar"))
            .navigateTo(parent(a))
            .build();
        final WebElement logoutParent = driver.findElement(logoutParentSelector);
        clickButton(logoutParent);
    }
}
