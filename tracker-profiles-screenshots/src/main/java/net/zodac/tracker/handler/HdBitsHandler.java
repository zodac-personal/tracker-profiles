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
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.TrackerType;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.gui.DisplayUtils;
import net.zodac.tracker.framework.xpath.NamedHtmlElement;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code HDBits} tracker.
 */
// TODO: Type is CF check AND manual, should both be listed?
@TrackerHandler(name = "HDBits", type = TrackerType.CLOUDFLARE_CHECK, url = {
    "https://hdbits.org/",
    "https://backup.hdbits.org/"
})
public class HdBitsHandler extends AbstractTrackerHandler {

    @Override
    protected By cloudflareSelector() {
        return DEFAULT_CLOUDFLARE_SELECTOR;
    }

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(input, withName("uname"), withType("text"))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(input, withName("password"), withType("password"))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link HdBitsHandler}, prior to clicking the login button with a successful username/password there is another field where an image needs
     * to be selected based on a text hint.
     *
     * <p>
     * Manual user interaction:
     * <ol>
     *     <li>Select the correct image</li>
     * </ol>
     */
    @Override
    protected void manualCheckBeforeLoginClick(final String trackerName) {
        final By twoFactorPasscodeSelector = XpathBuilder
            .from(input, withName("twostep_code"), withType("number"))
            .parent(td)
            .build();
        final WebElement twoFactorPasscodeElement = driver.findElement(twoFactorPasscodeSelector);
        scriptExecutor.highlightElement(twoFactorPasscodeElement);

        final By captchaSelector = XpathBuilder
            .from(div, withClass("captchaIntro"))
            .descendant(NamedHtmlElement.of("strong"), atIndex(1))
            .build();
        final WebElement captchaTextElement = driver.findElement(captchaSelector);
        LOGGER.info("\t\t >>> Waiting for user to select the '{}' image", captchaTextElement.getText());

        final WebElement captchaElement = driver.findElement(By.id("captcha"));
        scriptExecutor.highlightElement(captchaElement);

        DisplayUtils.userInputConfirmation(trackerName, String.format("Select the '%s' image (and enter 2FA passcode if enabled)",
            captchaTextElement.getText()));
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(input, withType("submit"))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        return By.id("news");
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(div, withClass("curuser-stats"), atIndex(1))
            .child(NamedHtmlElement.of("b"), atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link HdBitsHandler}, there is also a table entry with our passkey. We find the {@literal <}{@code tr}{@literal >} {@link WebElement}s
     * which has a {@literal <}{@code td}{@literal >} {@link WebElement} with the text value <b>Passkey</b>. From this
     * {@literal <}{@code tr}{@literal >}, we find the child {@literal <}{@code td}{@literal >}, which needs its content redacted.
     *
     * @see AbstractTrackerHandler#redactElements()
     * @see net.zodac.tracker.redaction.Redactor#redactPasskey(WebElement)
     */
    @Override
    public int redactElements() {
        final By passkeySelector = XpathBuilder
            .from(tr, withId("seclog"))
            .parent(tbody)
            .child(tr, atIndex(4))
            .child(td, atIndex(2))
            .build();
        final WebElement passkeyValueElement = driver.findElement(passkeySelector);
        redactor.redactPasskey(passkeyValueElement);

        return 1 + super.redactElements();
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            // IP address
            XpathBuilder
                .from(td, withClass("heading"))
                .parent(tr)
                .child(td, atIndex(2))
                .build(),
            // IP history
            XpathBuilder
                .from(tr, withId("seclog"))
                .descendant(tr)
                .child(td, atIndex(2))
                .build()
        );
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(div, withClass("curuser-stats"), atIndex(1))
            .child(a, atIndex(2))
            .build();
    }
}
