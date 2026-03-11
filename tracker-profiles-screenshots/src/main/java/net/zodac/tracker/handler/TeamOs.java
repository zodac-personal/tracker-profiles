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
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.containsAttribute;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.driver.extension.ExtensionBinding;
import net.zodac.tracker.framework.driver.extension.ExtensionSettings;
import net.zodac.tracker.framework.driver.extension.UblockOriginLiteExtension;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.handler.definition.UsesExtensions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Extension of the {@link Unit3dHandler} for the {@code TeamOS} tracker.
 */
@TrackerHandler(name = "TeamOS", url = "https://teamos.xyz/login/")
public class TeamOs extends XenForoHandler implements UsesExtensions {

    private static final int EXPECTED_NUMBER_OF_THREAD_LINKS = 1;

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link TeamOs}, the tracker can request you to read a thread after logging in. We'll explicitly search for this request to read a thread.
     * If it exists, we'll open the page (forcing it into the current tab). We'll then continue with the rest of the flow.
     */
    @Override
    protected void manualCheckAfterLoginClick(final String trackerName) {
        browserInteractionHelper.stopPageLoad();

        // When an update to site rules occurs, there is a single button with a link to the rules thread
        final By adminThreadButtonsSelector = XpathBuilder
            .from(a, withClass("button"), containsAttribute("href", "/threads/site-introduction-and-rules"))
            .build();
        final List<WebElement> adminThreadButtons = driver.findElements(adminThreadButtonsSelector).stream().toList();

        if (adminThreadButtons.size() != EXPECTED_NUMBER_OF_THREAD_LINKS) {
            LOGGER.debug("\t\t- Found {} thread links, not clicking anything", adminThreadButtons.size());
            return;
        }

        LOGGER.debug("\t\t- Found single thread link to updated rules, admin requires thread to be viewed, clicking...");
        final WebElement adminThreadButton = adminThreadButtons.getFirst();
        browserInteractionHelper.removeAttribute(adminThreadButton, "target"); // Stop forcing the link to open in a new tab
        clickButton(adminThreadButton);
        browserInteractionHelper.stopPageLoad();
        LOGGER.debug("\t\t- Updated rules thread viewed, continuing tracker execution");
    }

    // TODO: Have a before/after screenshot section, where this tracker's bespoke scrollbar can be explicitly hidden?

    @Override
    public List<ExtensionBinding<?>> requiredExtensions() {
        final ExtensionSettings<UblockOriginLiteExtension.UblockSettings> ublockOriginLiteExtensionSettings =
            () -> {
                final Map<UblockOriginLiteExtension.UblockSettings, Boolean> settings =
                    new EnumMap<>(UblockOriginLiteExtension.UblockSettings.class);
                settings.put(UblockOriginLiteExtension.UblockSettings.ENABLE_MISCELLANEOUS_FILTERS, true);
                settings.put(UblockOriginLiteExtension.UblockSettings.ENABLE_REGION_FILTERS, true);
                settings.put(UblockOriginLiteExtension.UblockSettings.SET_FILTERING_MODE, false);
                return settings;
            };

        return List.of(
            ExtensionBinding.of(new UblockOriginLiteExtension(), ublockOriginLiteExtensionSettings)
        );
    }
}
