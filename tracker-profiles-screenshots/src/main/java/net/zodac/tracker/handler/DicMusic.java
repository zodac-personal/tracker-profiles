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

import static net.zodac.tracker.framework.xpath.HtmlElement.div;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;

import java.util.List;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.handler.definition.HasJumpButtons;
import net.zodac.tracker.redaction.RedactionBuffer;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;

/**
 * Extension of the {@link GazelleHandler} for the {@code DICMusic} tracker.
 */
@TrackerHandler(name = "DICMusic", url = "https://dicmusic.com/")
public class DicMusic extends GazelleHandler implements HasJumpButtons {

    @Override
    public List<By> jumpButtonSelectors() {
        return List.of(
            By.id("back-to-top-btn")
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link DicMusic}, if the user has linked their {@code Last.fm} account, there is an element that loads the last played song, and this can
     * be slower to load than the rest of the page. So we check for the existence of the {@code Last.fm} username (which is always loaded), and if it
     * exists we wait for the 'Last played' value to load. This can sometimes not load at all, so we will wait for the element here before returning.
     * If it doesn't load, we will log a warning but continue execution.
     *
     * <p>
     * If there is no {@code Last.fm} username, we use the {@link #profilePageContentSelector()} from {@link GazelleHandler}.
     */
    @Override
    protected By profilePageContentSelector() {
        try {
            LOGGER.debug("\t\t- Looking for Last.fm section for user");
            final By lastFmSectionSelector = XpathBuilder
                .from(div, withClass("box_lastfm"))
                .build();
            driver.findElement(lastFmSectionSelector);

            LOGGER.debug("\t\t- Found Last.fm section, waiting for 'last played' entry to load");
            final By lastFmLastPlayedSelector = By.id("lastfm_stats");
            browserInteractionHelper.waitForElementToBeVisible(lastFmLastPlayedSelector, pageLoadDuration());
            return lastFmLastPlayedSelector;
        } catch (final NoSuchElementException e) {
            LOGGER.debug("\t\t- Found no Last.fm section, assuming user has not linked account, will use fallback item to confirm profile page", e);
            return super.profilePageContentSelector();
        } catch (final TimeoutException e) {
            LOGGER.warn("\t\t- Unable to find Last.fm 'last played' section");
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The redaction doesn't cover the full {@code <li>} element for some reason, so we extend it to the left.
     */
    @Override
    protected RedactionBuffer emailElementBuffer() {
        return RedactionBuffer.withLeftOffset(7);
    }
}
