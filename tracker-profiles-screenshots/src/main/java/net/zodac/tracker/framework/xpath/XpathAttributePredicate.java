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
 * Utility {@link XpathPredicate} functions to filter HTML elements by attribute for an XPath query.
 *
 * @see XpathBuilder
 */
public final class XpathAttributePredicate {

    private XpathAttributePredicate() {

    }

    /**
     * Update the XPATH query to specify the index of the element at the current position.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query[@attributeName='attributeValue']<u>[index]</u>
     * </pre>
     *
     * @param index the index of the current HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate atIndex(final int index) {
        return xpath -> xpath.append('[').append(index).append(']');
    }

    /**
     * Update the XPATH query to specify first element at the current position.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query[@attributeName='attributeValue']<u>[1]</u>
     * </pre>
     *
     * @return this {@link XpathAttributePredicate}
     * @see #atIndex(int)
     */
    public static XpathPredicate atFirstIndex() {
        return atIndex(1);
    }

    /**
     * Update the XPATH query to filter the element at the current position if it contains the provided attribute.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query<u>[contains(@attribute-name, 'attribute-value')]</u>
     * </pre>
     *
     * @param name  the name of the wanted attribute for the HTML element
     * @param value the value of the wanted attribute for the HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate containsAttribute(final String name, final String value) {
        return containsAttributeFunction("@" + name, value);
    }

    /**
     * Update the XPATH query to filter the element at the current position if it contains the provided attribute function.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query<u>[contains(attribute-function(), 'attribute-value')]</u>
     * </pre>
     *
     * @param name  the name of the wanted attribute function for the HTML element
     * @param value the value of the wanted attribute function for the HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate containsAttributeFunction(final String name, final String value) {
        return xpath ->
            xpath.append("[contains(")
                .append(name)
                .append(", '")
                .append(escapeXpath(value))
                .append("')]");
    }

    /**
     * Update the XPATH query to filter the element at the current position if it has an exact match for the provided attribute.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query<u>[@attribute-name='attribute-value']</u>
     * </pre>
     *
     * @param name  the name of the wanted attribute for the HTML element
     * @param value the value of the wanted attribute for the HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate withAttribute(final String name, final String value) {
        return withAttributeFunction("@" + name, value);
    }

    /**
     * Update the XPATH query to filter the element at the current position if it has an exact match for the provided attribute function.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query<u>[attribute-function()='attribute-value']</u>
     * </pre>
     *
     * @param name  the name of the wanted attribute function for the HTML element
     * @param value the value of the wanted attribute function for the HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate withAttributeFunction(final String name, final String value) {
        return xpath ->
            xpath.append('[')
                .append(name)
                .append("='")
                .append(escapeXpath(value))
                .append("']");
    }

    /**
     * Update the XPATH query to filter the element at the current position if it contains the provided {@code class} attribute.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query<u>[contains(@class, 'wanted-class')]</u>
     * </pre>
     *
     * @param value the wanted {@code class} for the HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate withClass(final String value) {
        return containsAttribute("class", value);
    }

    /**
     * Update the XPATH query to filter the element at the current position if it has an exact match for the provided {@code id} attribute.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query<u>[@id='id']</u>
     * </pre>
     *
     * @param value the wanted {@code id} for the HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate withId(final String value) {
        return withAttribute("id", value);
    }

    /**
     * Update the XPATH query to filter the element at the current position if it contains the provided {@code name} attribute.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query<u>[@name='wanted-name']</u>
     * </pre>
     *
     * @param value the wanted {@code name} for the HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate withName(final String value) {
        return withAttribute("name", value);
    }

    /**
     * Update the XPATH query to filter the element at the current position to only return elements which contain the supplied text.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query<u>[contains(normalize-space(), 'wanted text')]</u>
     * </pre>
     *
     * @param value the wanted text for the HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate withText(final String value) {
        return containsAttributeFunction("normalize-space()", value);
    }

    /**
     * Update the XPATH query to filter the element at the current position if it contains the provided {@code type} attribute.
     *
     * <p>
     * Adds:
     *
     * <pre>
     *      //existing_query<u>[@type='wanted-type']</u>
     * </pre>
     *
     * @param value the wanted {@code type} for the HTML element
     * @return this {@link XpathAttributePredicate}
     */
    public static XpathPredicate withType(final String value) {
        return withAttribute("type", value);
    }

    private static String escapeXpath(final String value) {
        return value.replace("'", "\\'");
    }
}
