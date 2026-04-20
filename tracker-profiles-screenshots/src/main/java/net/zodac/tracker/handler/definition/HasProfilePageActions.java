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

package net.zodac.tracker.handler.definition;

/**
 * Marks an {@link net.zodac.tracker.handler.AbstractTrackerHandler} as requiring additional actions after opening the profile page, but prior to the
 * page being redacted and screenshot. This might be that the page is considered 'loaded' but the required {@link org.openqa.selenium.WebElement}s are
 * not all on the screen, or that some {@link org.openqa.selenium.WebElement}s may need to be interacted with prior to the screenshot.
 */
@FunctionalInterface
public interface HasProfilePageActions {

    /**
     * Performs any additional actions required on the user profile page before redaction and screenshot.
     */
    void performActionOnProfilePage();
}
