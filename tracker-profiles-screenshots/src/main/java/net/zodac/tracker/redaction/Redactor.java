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

import org.openqa.selenium.WebElement;

/**
 * Interface defining available redaction methods. These can be used prior to taking a screenshot of a tracker profile page to hide sensitive user
 * information.
 */
public interface Redactor {

    /**
     * Redacts the entire {@link WebElement}.
     *
     * @param element     the {@link WebElement} to redact
     * @param description the {@link String} to describe what the sensitive information is
     */
    void redact(WebElement element, String description);

    /**
     * Redacts the user's passkey.
     *
     * @param element the {@link WebElement} containing the user's passkey
     */
    void redactPasskey(WebElement element);

    /**
     * Redacts the user's email address.
     *
     * @param element the {@link WebElement} containing the user's email
     */
    void redactEmail(WebElement element);

    /**
     * Redacts the user's IP address.
     *
     * @param element the {@link WebElement} containing the user's IP address.
     */
    void redactIpAddress(WebElement element);

}
