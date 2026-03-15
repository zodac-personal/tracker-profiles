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

/**
 * Represents an XPath axis navigation step that can be applied to an XPath expression being constructed.
 *
 * <p>
 * Unlike {@link XpathPredicate} (which appends filter expressions within {@code []} brackets), implementations of this interface append
 * axis-based navigation steps such as {@code /ancestor::}, {@code /following-sibling::}, or {@code /preceding-sibling::}.
 *
 * @see XpathBuilder
 * @see XpathAxis
 */
@FunctionalInterface
public interface XpathNavigation {

    /**
     * Applies this axis navigation step to the supplied XPath expression.
     *
     * <p>
     * Implementations should append valid XPath axis syntax to the provided {@link StringBuilder}.
     *
     * @param xpath the {@link StringBuilder} representing the XPath expression being constructed
     */
    void apply(StringBuilder xpath);
}
