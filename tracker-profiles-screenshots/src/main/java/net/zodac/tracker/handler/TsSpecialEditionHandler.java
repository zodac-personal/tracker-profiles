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
import static net.zodac.tracker.framework.xpath.HtmlElement.form;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.annotation.CommonTrackerHandler;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.NamedHtmlElement;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Common implementation of {@link AbstractTrackerHandler} for {@code TS Special Edition}-based trackers.
 */
@CommonTrackerHandler("TSSpecialEdition")
@TrackerHandler(name = "ImmortalSeed", url = "https://immortalseed.me/")
@TrackerHandler(name = "Tasmanites", url = "https://tasmanit.es/")
public class TsSpecialEditionHandler extends AbstractTrackerHandler {

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(input, withClass("inputUsernameLoginbox"))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(input, withClass("inputPasswordLoginbox"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(NamedHtmlElement.any(), withId("collapseobj_loginbox"))
            .descendant(form, atIndex(1))
            .child(input, atIndex(4))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        return By.id("collapseobj_loginbox");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Depending on the specific {@link TsSpecialEditionHandler} tracker, the <b>collapseobj_loginbox</b> HTML element is sometimes a
     * {@literal <}div{@literal >}, sometimes a {@literal <}table{@literal >}. In addition, the user profile link is sometimes a direct child if this
     * element, or sometimes nested below other HTML elements. This general XPATH query should work for all trackers of this type.
     *
     * @return the profile page {@link By} selector
     */
    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(NamedHtmlElement.any(), withId("collapseobj_loginbox"))
            .descendant(a, atIndex(1))
            .build();
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            // Email and IP address
            XpathBuilder
                .from(tbody)
                .child(tr, atIndex(1))
                .child(td, atIndex(1))
                .build(),
            // IP address in header
            XpathBuilder
                .from(div, withId("top"))
                .child(div, atIndex(2))
                .child(span)
                .build()
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link TsSpecialEditionHandler}, after clicking the logout button, a Javascript alert appears, which must be accepted.
     */
    @Override
    public void logout() {
        final By logoutButtonSelector = logoutButtonSelector();
        scriptExecutor.waitForElementToAppear(logoutButtonSelector, DEFAULT_WAIT_FOR_PAGE_LOAD);
        final WebElement logoutButton = driver.findElement(logoutButtonSelector);
        clickButton(logoutButton);

        // After clicking logout, an alert appears - find and click 'Yes'
        scriptExecutor.acceptAlert();

        scriptExecutor.waitForPageToLoad(DEFAULT_WAIT_FOR_PAGE_LOAD);
        scriptExecutor.waitForElementToAppear(postLogoutElementSelector(), DEFAULT_WAIT_FOR_PAGE_LOAD);
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(div, withId("top"))
            .child(div, atIndex(2))
            .child(span, atIndex(1))
            .child(a, atIndex(2))
            .build();
    }
}
