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

package net.zodac.tracker.framework.xpath;

import java.util.Locale;

/**
 * Represents an HTML element identified solely by its tag name.
 *
 * <p>
 * This type is intended for elements that are not included in {@link HtmlElement}, such as uncommon HTML tags, SVG elements, web components, or
 * wildcard selections.
 *
 * <p>
 * Unlike {@link HtmlElement}, this class allows arbitrary tag names while still preserving a strongly-typed API and avoiding raw {@link String}
 * usage throughout the {@link XpathBuilder}.
 *
 * <p>
 * Instances should be created via the provided factory methods to ensure basic validation and consistent behavior.
 *
 * @param tagName the element tag name
 */
public record NamedHtmlElement(String tagName) implements Element {

    /**
     * Creates a {@link NamedHtmlElement} for the given {@code tagName}.
     *
     * @param tagName the element tag name
     * @return a new {@link NamedHtmlElement}
     * @throws IllegalArgumentException if the {@code tagName} is blank
     */
    public static NamedHtmlElement of(final String tagName) {
        if (tagName.isBlank()) {
            throw new IllegalArgumentException("Input cannot be blank");
        }

        return new NamedHtmlElement(tagName);
    }

    /**
     * Creates a wildcard {@link NamedHtmlElement} that matches any tag ({@code *}) in an XPath expression.
     *
     * @return a wildcard {@link NamedHtmlElement}
     */
    public static NamedHtmlElement any() {
        return of("*");
    }

    @Override
    public String tagName() {
        return tagName.toLowerCase(Locale.getDefault());
    }
}
