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

/**
 * Binds an {@link Extension} with its corresponding {@link ExtensionSettings}.
 *
 * <p>
 * This record ensures that each {@link Extension} is associated with its {@link ExtensionSettings}, guaranteeing that the {@link ExtensionSettings}
 * enum matches the {@link Extension} expected enum type.
 *
 * @param extension the {@link Extension}
 * @param settings  the {@link ExtensionSettings} associated with the {@link Extension}
 * @param <E>       the enum type representing the available settings for the {@link Extension}
 */
public record ExtensionBinding<E extends Enum<E>>(Extension<E> extension, ExtensionSettings<E> settings) {

    /**
     * Creates a new {@link ExtensionBinding} instance for the given{@link Extension} and its {@link ExtensionSettings}.
     *
     * @param extension         the {@link Extension}
     * @param extensionSettings the {@link ExtensionSettings} associated with the {@link Extension}
     * @param <E>               the enum type representing the available settings for the {@link Extension}
     * @return a new {@link ExtensionBinding} binding the extension to its settings
     */
    public static <E extends Enum<E>> ExtensionBinding<E> of(final Extension<E> extension, final ExtensionSettings<E> extensionSettings) {
        return new ExtensionBinding<>(extension, extensionSettings);
    }
}
