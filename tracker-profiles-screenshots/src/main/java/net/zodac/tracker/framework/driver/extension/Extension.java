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

package net.zodac.tracker.framework.driver.extension;

import net.zodac.tracker.util.BrowserInteractionHelper;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Interface defining a web browser extension.
 *
 * @param <E> the type of the {@link ExtensionSettings}
 */
public interface Extension<E extends Enum<E>> {

    /**
     * The ID of the {@link Extension}.
     *
     * @return the ID
     */
    String id();

    /**
     * The path to the {@link Extension} {@code .crx} file.
     *
     * @return the filepath
     */
    String path();

    /**
     * Performs any configuration needed for the {@link Extension}.
     *
     * @param extensionSettings the {@link ExtensionSettings} for this {@link Extension}
     * @param driver            the {@link RemoteWebDriver}
     * @param browserInteractionHelper    the {@link BrowserInteractionHelper}
     */
    void configure(ExtensionSettings<E> extensionSettings, RemoteWebDriver driver, BrowserInteractionHelper browserInteractionHelper);
}
