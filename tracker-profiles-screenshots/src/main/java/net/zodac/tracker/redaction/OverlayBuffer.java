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

package net.zodac.tracker.redaction;

/**
 * Defines the pixel buffers applied to each side of an overlay box when performing {@link OverlayRedactor} redaction. A positive value expands the
 * overlay beyond the element's bounding rectangle in that direction, while a negative value contracts it.
 *
 * @param up    pixels to extend the overlay above the element
 * @param down  pixels to extend the overlay below the element
 * @param left  pixels to extend the overlay to the left of the element
 * @param right pixels to extend the overlay to the right of the element
 */
public record OverlayBuffer(int up, int down, int left, int right) {

    /**
     * Default {@link OverlayBuffer} used when no custom buffer is required.
     */
    public static final OverlayBuffer DEFAULT = new OverlayBuffer(0, 0, 0, 0);

    /**
     * Static constructor to create an {@link OverlayBuffer}.
     *
     * @param up    pixels to extend the overlay above the element
     * @param down  pixels to extend the overlay below the element
     * @param left  pixels to extend the overlay to the left of the element
     * @param right pixels to extend the overlay to the right of the element
     * @return the created {@link OverlayBuffer}
     */
    public static OverlayBuffer of(final int up, final int down, final int left, final int right) {
        return new OverlayBuffer(up, down, left, right);
    }

    /**
     * Static constructor to create an {@link OverlayBuffer} with only a left offset.
     *
     * @param leftOffset pixels to extend the overlay to the left of the element
     * @return the created {@link OverlayBuffer}
     */
    public static OverlayBuffer withLeftOffset(final int leftOffset) {
        return of(0, 0, leftOffset, 0);
    }

    /**
     * Static constructor to create an {@link OverlayBuffer} with only a right offset.
     *
     * @param rightOffset pixels to extend the overlay to the right of the element
     * @return the created {@link OverlayBuffer}
     */
    public static OverlayBuffer withRightOffset(final int rightOffset) {
        return of(0, 0, 0, rightOffset);
    }
}
