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

package net.zodac.tracker.framework.gui;

/**
 * Defines the screen position of a {@link DisplayUtils} dialog as percentages of the available screen space.
 *
 * <p>Each value must be in the range {@code [0, 100]}:
 * <ul>
 *   <li>{@code horizontalPercent}: {@code 0} places the dialog at the left edge, {@code 100} places it at the right edge.</li>
 *   <li>{@code verticalPercent}: {@code 0} places the dialog at the top, {@code 100} at the bottom.</li>
 * </ul>
 */
public final class DialogPosition {

    private static final int MIN_PERCENT = 0;
    private static final int MAX_PERCENT = 100;
    private static final int DEFAULT_HORIZONTAL_PERCENT = 0;
    private static final int DEFAULT_VERTICAL_PERCENT = 60;

    private final int horizontalPercent;
    private final int verticalPercent;

    private DialogPosition(final int horizontalPercent, final int verticalPercent) {
        this.horizontalPercent = horizontalPercent;
        this.verticalPercent = verticalPercent;
    }

    /**
     * Creates a {@link DialogPosition} with the given percentage coordinates.
     *
     * @param horizontalPercent percentage (0–100) of the available horizontal space for the dialog's left edge
     * @param verticalPercent   percentage (0–100) of the available vertical space for the dialog's top edge
     * @return the {@link DialogPosition}
     * @throws IllegalArgumentException if either value is outside {@code [0, 100]}
     */
    public static DialogPosition of(final int horizontalPercent, final int verticalPercent) {
        if (horizontalPercent < MIN_PERCENT || horizontalPercent > MAX_PERCENT) {
            throw new IllegalArgumentException("horizontalPercent must be in [0, 100], got: %d".formatted(horizontalPercent));
        }
        if (verticalPercent < MIN_PERCENT || verticalPercent > MAX_PERCENT) {
            throw new IllegalArgumentException("verticalPercent must be in [0, 100], got: %d".formatted(verticalPercent));
        }
        return new DialogPosition(horizontalPercent, verticalPercent);
    }

    /**
     * Returns the default dialog position: left-aligned (0%), 60% down the screen.
     *
     * @return the default {@link DialogPosition}
     */
    public static DialogPosition ofDefault() {
        return of(DEFAULT_HORIZONTAL_PERCENT, DEFAULT_VERTICAL_PERCENT);
    }

    /**
     * Returns the horizontal placement as a percentage (0–100) of the available screen width.
     *
     * @return the horizontal percentage
     */
    public int horizontalPercent() {
        return horizontalPercent;
    }

    /**
     * Returns the vertical placement as a percentage (0–100) of the available screen height.
     *
     * @return the vertical percentage
     */
    public int verticalPercent() {
        return verticalPercent;
    }
}
