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
 * Represents a curated set of well-known HTML elements that can be used when constructing XPath expressions.
 *
 * <p>
 * Using {@code HtmlElement} provides compile-time safety and IDE auto-completion for common HTML tags, avoiding the need to rely on raw
 * {@link String} literals.
 *
 * <p>
 * Each enum constant maps directly to its corresponding lowercase HTML tag name when rendered into an XPath expression.
 *
 * <p>
 * For elements that are not part of this predefined set (e.g. SVG tags, web components, or uncommon elements), use {@link NamedHtmlElement} instead.
 *
 * @see Element
 * @see NamedHtmlElement
 */
@SuppressWarnings("PMD") // Suppressing PMD since the enum values are not uppercase, but lowercase makes more sense for these values
public enum HtmlElement implements Element {

    /**
     * Anchor {@link Element}: {@code {@literal <}a{@literal >}}.
     */
    a,

    /**
     * Button {@link Element}: {@code {@literal <}button{@literal >}}.
     */
    button,

    /**
     * Generic container {@link Element}: {@code {@literal <}div{@literal >}}.
     */
    div,

    /**
     * Form {@link Element}: {@code {@literal <}form{@literal >}}.
     */
    form,

    /**
     * Image {@link Element}: {@code {@literal <}img{@literal >}}.
     */
    img,

    /**
     * Input control {@link Element}: {@code {@literal <}input{@literal >}}.
     */
    input,

    /**
     * List item {@link Element}: {@code {@literal <}li{@literal >}}.
     */
    li,

    /**
     * Inline container {@link Element}: {@code {@literal <}span{@literal >}}.
     */
    span,

    /**
     * Table {@link Element}: {@code {@literal <}table{@literal >}}.
     */
    table,

    /**
     * Table body {@link Element}: {@code {@literal <}tbody{@literal >}}.
     */
    tbody,

    /**
     * Table cell {@link Element}: {@code {@literal <}td{@literal >}}.
     */
    td,

    /**
     * Table header {@link Element}: {@code {@literal <}th{@literal >}}.
     */
    th,

    /**
     * Table row {@link Element}: {@code {@literal <}tr{@literal >}}.
     */
    tr,

    /**
     * Unordered list {@link Element}: {@code {@literal <}ul{@literal >}}.
     */
    ul;

    @Override
    public String tagName() {
        return name().toLowerCase(Locale.getDefault());
    }
}
