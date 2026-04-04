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
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link Redactor} that redacts text by applying a Gaussian blur directly to the sensitive content within the impacted
 * {@link WebElement}.
 */
class BlurRedactor implements Redactor {

    private static final String BLUR_DEFINITION = "blur(0.5em)";
    private static final String IRC_KEY_PREFIX_ALTERNATION = "IRC Key";
    private static final String TORRENT_PASSKEY_PREFIX_ALTERNATION = "Passkey|Pass Key";

    private static final String REDACT_ELEMENT_SCRIPT = Redactor.loadScript("redact_element_blur.js");
    private static final String REDACT_EMAIL_SCRIPT = Redactor.loadScript("redact_email_blur.js");
    private static final String REDACT_IP_ADDRESS_SCRIPT = Redactor.loadScript("redact_ip_address_blur.js");
    private static final String REDACT_IRC_PASSKEY_SCRIPT = Redactor.loadScript("redact_irc_passkey_blur.js");
    private static final String REDACT_TORRENT_PASSKEY_SCRIPT = Redactor.loadScript("redact_torrent_passkey_blur.js");

    private final RemoteWebDriver driver;

    /**
     * Default constructor.
     *
     * @param driver the {@link RemoteWebDriver}
     */
    BlurRedactor(final RemoteWebDriver driver) {
        this.driver = driver;
    }

    @Override
    public int redact(final WebElement element, final String description, final RedactionBuffer buffer) {
        final String script = REDACT_ELEMENT_SCRIPT.formatted(BLUR_DEFINITION);
        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactEmail(final WebElement element, final RedactionBuffer buffer) {
        final String script = REDACT_EMAIL_SCRIPT.formatted(BLUR_DEFINITION);
        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactIpAddress(final WebElement element, final RedactionBuffer buffer) {
        final String script = REDACT_IP_ADDRESS_SCRIPT.formatted(BLUR_DEFINITION);
        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactIrcPasskey(final WebElement element, final RedactionBuffer buffer) {
        final String script = REDACT_IRC_PASSKEY_SCRIPT.formatted(IRC_KEY_PREFIX_ALTERNATION, BLUR_DEFINITION);
        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactTorrentPasskey(final WebElement element, final RedactionBuffer buffer) {
        final String script = REDACT_TORRENT_PASSKEY_SCRIPT.formatted(TORRENT_PASSKEY_PREFIX_ALTERNATION, BLUR_DEFINITION);
        driver.executeScript(script, element);
        return 1;
    }
}
