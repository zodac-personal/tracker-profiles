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

import java.util.List;
import org.openqa.selenium.WebElement;

/**
 * Interface defining available redaction methods. These can be used prior to taking a screenshot of a tracker profile page to hide sensitive user
 * information.
 */
public interface Redactor {

    /**
     * The known prefixes that may appear before an IRC passkey value. When one of these prefixes is found at the start of an element's text
     * (case-insensitive), it is preserved and only the value following it is redacted by {@link #redactIrcPasskey(WebElement, RedactionBuffer)}.
     */
    List<String> IRC_KEY_PREFIXES = List.of("IRC Key");

    /**
     * Redacts the entire {@link WebElement}.
     *
     * @param element     the {@link WebElement} to redact
     * @param description the {@link String} to describe what the sensitive information is
     * @param buffer      the {@link RedactionBuffer} defining the pixel expansion on each side of the redaction, if needed
     */
    // TODO: Add explicit entry for IRC keys, and look for 'IRC Key', etc. as a prefix and retain it when redacting
    void redact(WebElement element, String description, RedactionBuffer buffer);

    /**
     * Redacts the user's email address.
     *
     * @param element the {@link WebElement} containing the user's email
     * @param buffer  the {@link RedactionBuffer} defining the pixel expansion on each side of the redaction, if needed
     */
    void redactEmail(WebElement element, RedactionBuffer buffer);

    /**
     * Redacts the user's IP address.
     *
     * @param element the {@link WebElement} containing the user's IP address
     * @param buffer  the {@link RedactionBuffer} defining the pixel expansion on each side of the redaction, if needed
     */
    void redactIpAddress(WebElement element, RedactionBuffer buffer);

    /**
     * Redacts the user's IRC passkey.
     *
     * <p>
     * If the element's text begins with one of the prefixes in {@link #IRC_KEY_PREFIXES} (case-insensitive), that prefix is preserved and only the
     * value after it is redacted.
     *
     * @param element the {@link WebElement} to redact
     * @param buffer  the {@link RedactionBuffer} defining the pixel expansion on each side of the redaction, if needed
     */
    void redactIrcPasskey(WebElement element, RedactionBuffer buffer);

    /**
     * Redacts the user's torrent passkey.
     *
     * @param element the {@link WebElement} containing the user's passkey
     * @param buffer  the {@link RedactionBuffer} defining the pixel expansion on each side of the redaction, if needed
     */
    // TODO: Look for 'Passkey', 'Pass Key', etc. as a prefix and retain it when redacting
    void redactTorrentPasskey(WebElement element, RedactionBuffer buffer);
}
