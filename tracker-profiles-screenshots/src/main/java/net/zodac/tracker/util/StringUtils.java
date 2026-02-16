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

import java.util.Collection;

/**
 * Utility class with {@link String}-based functions.
 */
public final class StringUtils {

    private StringUtils() {

    }

    /**
     * Returns the plural suffix ({@code "s"}) if the provided count is not equal to {@code 1}.
     *
     * @param count the number of elements
     * @return an empty string if {@code count == 1}, otherwise {@code "s"}
     */
    public static String pluralise(final int count) {
        return count == 1 ? "" : "s";
    }

    /**
     * Returns the plural suffix ({@code "s"}) if the provided collection size is not equal to {@code 1}.
     *
     * @param collection the collection whose size determines pluralisation
     * @param <E>        the element type of the collection
     * @return an empty string if the collection size is {@code 1}, otherwise {@code "s"}
     * @see #pluralise(int)
     */
    public static <E> String pluralise(final Collection<E> collection) {
        return pluralise(collection.size());
    }
}
