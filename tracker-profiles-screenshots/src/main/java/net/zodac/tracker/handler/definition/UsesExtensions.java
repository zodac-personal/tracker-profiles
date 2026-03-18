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

package net.zodac.tracker.handler.definition;

import java.util.List;
import net.zodac.tracker.framework.driver.extension.Extension;

/**
 * Marks an {@link net.zodac.tracker.handler.AbstractTrackerHandler} as requiring browser {@link Extension}s. Implementing handlers must provide the
 * list of {@link Extension}s to be installed before execution begins.
 */
@FunctionalInterface
public interface UsesExtensions {

    /**
     * The {@link Extension}s required for the tracker. Any provided extensions will be installed in the
     * {@link org.openqa.selenium.remote.RemoteWebDriver} and configured prior to the main execution.
     *
     * @return the {@link List} of required {@link Extension}s
     */
    List<Extension> requiredExtensions();
}
