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

package net.zodac.tracker.framework;

import java.util.Arrays;
import java.util.Collection;
import net.zodac.tracker.framework.annotation.TrackerHandler;

/**
 * Simple class providing the defintion of a tracker for this application.
 *
 * @param name the name of the tracker, used to retrieve a {@link TrackerHandler} implementation
 * @param type the type of the tracker, used to define the type of {@link org.openqa.selenium.remote.RemoteWebDriver} to be used
 * @param urls the URLs to access the tracker
 */
public record TrackerDefinition(String name, TrackerType type, Collection<String> urls) {

    /**
     * Static constructor using the {@link TrackerHandler} annotation.
     *
     * @param trackerHandler the {@link TrackerHandler}
     * @return the created {@link TrackerDefinition}
     */
    public static TrackerDefinition fromAnnotation(final TrackerHandler trackerHandler) {
        return new TrackerDefinition(trackerHandler.name(), trackerHandler.type(), Arrays.asList(trackerHandler.url()));
    }
}
