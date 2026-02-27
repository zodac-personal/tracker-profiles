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

import java.util.Map;

/**
 * Represents the configuration settings for a specific {@link Extension}.
 *
 * @param <E> the enum type representing the available settings for the extension
 */
@FunctionalInterface
public interface ExtensionSettings<E extends Enum<E>> {

    /**
     * Returns thesettings for this {@link Extension}.
     *
     * <p>
     * Each key in the returned {@link Map} corresponds to a specific option defined by the extension’s enum type, and the value indicates whether
     * the option is enabled ({@code true}) or disabled ({@code false}).
     *
     * @return an {@link Map} of settings and their states
     */
    Map<E, Boolean> settings();
}
