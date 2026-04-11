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

package net.zodac.tracker.util;

import org.openqa.selenium.WebElement;

/**
 * Utility class for working with {@link WebElement}s.
 */
// TODO: Move anything from BrowserInteractionHelper that doesn't rely on a driver into this class
public final class WebElementUtils {

    private WebElementUtils() {

    }

    /**
     * todo.
     *
     * @param element the {@link WebElement}
     * @return the {@code textContent}, or an empty {@link String} if the value is {@code null}
     */
    public static String getTextContent(final WebElement element) {
        final String text = element.getText();
        if (!text.isEmpty()) {
            return text;
        }

        final String value = element.getAttribute("value");
        if (value != null && !value.isEmpty()) {
            return value;
        }

        final String textContent = element.getAttribute("textContent");
        return textContent == null ? "" : textContent;
    }
}
