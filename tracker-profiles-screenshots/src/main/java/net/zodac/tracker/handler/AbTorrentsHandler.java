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
import static net.zodac.tracker.framework.xpath.HtmlElement.li;
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withId;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code ABTorrents} tracker.
 */
@TrackerHandler(name = "ABTorrents", url = "https://usefultrash.net/")
public class AbTorrentsHandler extends AbstractTrackerHandler {

    // Tab title is in the format `ABTorrents :(1): Home`, where `1` is the number of unread messages
    private static final Pattern TITLE_UNREAD_MESSAGES_COUNT_PATTERN = Pattern.compile("\\((\\d+)\\)");
    private static final Duration WAIT_FOR_TAB_TITLE_UPDATE = Duration.of(2L, ChronoUnit.SECONDS);

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(input, withName("username"), withType("text"))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(input, withName("password"), withType("password"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(input, withName("submitme"), withType("submit"))
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link AbTorrentsHandler}, having any unread private messages means you are unable to search for any torrents. While this doesn't block
     * the profile page, we'll click the link to the inbox then open any unread private messages before continuing.
     */
    // TODO: Remove this step, no longer needed for this tracker
    @Override
    protected void manualCheckAfterLoginClick(final String trackerName) {
        ScriptExecutor.explicitWait(WAIT_FOR_TAB_TITLE_UPDATE); // Waiting for tab to update with unread private messages count

        if (!hasUserAnyUnreadPrivateMessages()) {
            LOGGER.debug("\t- No unread private messages");
            return;
        }

        LOGGER.trace("\t- Found some unread private messages, opening inbox");
        // Highlight the user menu to make the logout button interactable
        final By messagesParentSelector = By.id("user");
        final WebElement messagesParent = driver.findElement(messagesParentSelector);
        scriptExecutor.moveTo(messagesParent);

        final By messagesSelector = XpathBuilder
            .from(li, withId("user"))
            .child(li, atIndex(1))
            .child(li, atIndex(1))
            .child(a, atIndex(1))
            .build();
        final WebElement messagesElement = driver.findElement(messagesSelector);
        clickButton(messagesElement);

        // Assuming that all 'Unread' <span> elements have the 'has-text-red' class to indicate they are unread, and that can be used instead of text
        final By unreadPrivateMessagesSelector = XpathBuilder
            .from(tr)
            .child(td, atIndex(2))
            .child(span, withClass("has-text-red"))
            .parent(td)
            .child(a, atIndex(1))
            .build();
        final List<WebElement> unreadPrivateMessages = driver.findElements(unreadPrivateMessagesSelector);
        LOGGER.debug("\t- {} unread private message{}", unreadPrivateMessages.size(), unreadPrivateMessages.size() == 1 ? "" : "s");

        for (final WebElement unreadPrivateMessage : unreadPrivateMessages) {
            clickButton(unreadPrivateMessage);
            driver.navigate().back();
        }

        LOGGER.debug("\t\t- Unread private messages cleared");
    }

    private boolean hasUserAnyUnreadPrivateMessages() {
        final String currentTitle = driver.getTitle() == null ? "" : driver.getTitle();
        LOGGER.trace("\t- Browser title: '{}'", currentTitle);
        final Matcher matcher = TITLE_UNREAD_MESSAGES_COUNT_PATTERN.matcher(currentTitle);

        if (matcher.find()) {
            final int numberOfUnreadPrivateMessages = Integer.parseInt(matcher.group(1));
            return numberOfUnreadPrivateMessages > 0;
        }

        // No number found, assume 0
        return false;
    }

    @Override
    protected By postLoginSelector() {
        return By.id("base_usermenu");
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(div, withId("base_usermenu"))
            .child(div, atIndex(1))
            .child(span, atIndex(1))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            // Last connected IP address
            XpathBuilder
                .from(span, withClass("has-text-green"))
                .build()
        );
    }

    @Override
    protected By logoutButtonSelector() {
        ScriptExecutor.explicitWait(Duration.ofSeconds(1L)); // Wait for the logout button to become visible and clickable again after scrolling
        return By.id("logoff");
    }
}
