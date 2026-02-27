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

import static net.zodac.tracker.framework.xpath.HtmlElement.img;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withName;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withType;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.driver.extension.ExtensionBinding;
import net.zodac.tracker.framework.driver.extension.ExtensionSettings;
import net.zodac.tracker.framework.driver.extension.UblockOriginLiteExtension;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code RUTracker} tracker.
 */
@TrackerHandler(name = "RUTracker", url = {
    // Link direct to the tracker page
    "https://rutracker.org/forum/tracker.php",
    "https://rutracker.net/forum/tracker.php"
})
public class RuTrackerHandler extends AbstractTrackerHandler {

    @Override
    protected By usernameFieldSelector() {
        return XpathBuilder
            .from(table, withClass("forumline"))
            .descendant(input, withName("login_username"), withType("text"))
            .build();
    }

    @Override
    protected By passwordFieldSelector() {
        return XpathBuilder
            .from(table, withClass("forumline"))
            .descendant(input, withName("login_password"), withType("password"))
            .build();
    }

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(table, withClass("forumline"))
            .descendant(input, withName("login"), withType("submit"))
            .build();
    }

    @Override
    protected By profilePageSelector() {
        return By.id("logged-in-username");
    }

    @Override
    public boolean hasSensitiveInformation() {
        return false;
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(img, withClass("log-out-icon"))
            .build();
    }

    @Override
    protected By postLogoutElementSelector() {
        return By.id("quick-search-guest");
    }

    @Override
    protected Duration maximumClickResolutionDuration() {
        return Duration.ofMinutes(2L);
    }

    @Override
    protected Duration waitForPageLoadDuration() {
        return Duration.ofMinutes(1L);
    }

    @Override
    protected List<ExtensionBinding<?>> requiredExtensions() {
        final ExtensionSettings<UblockOriginLiteExtension.UblockSettings> ublockOriginLiteExtensionSettings =
            () -> {
                final Map<UblockOriginLiteExtension.UblockSettings, Boolean> settings =
                    new EnumMap<>(UblockOriginLiteExtension.UblockSettings.class);
                settings.put(UblockOriginLiteExtension.UblockSettings.ENABLE_MISCELLANOUS_FILTERS, true);
                settings.put(UblockOriginLiteExtension.UblockSettings.ENABLE_REGION_FILTERS, true);
                settings.put(UblockOriginLiteExtension.UblockSettings.SET_FILTERING_MODE, true);
                return settings;
            };

        return List.of(
            ExtensionBinding.of(new UblockOriginLiteExtension(), ublockOriginLiteExtensionSettings)
        );
    }
}
