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

import java.util.regex.Pattern;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link Redactor} that delegates calls to another concrete implementation. We use {@link ApplicationConfiguration#redactionType()}
 * to determine which to use.
 */
public class RedactorImpl implements Redactor {

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");
    private static final ApplicationConfiguration CONFIG = Configuration.get();
    private static final Logger LOGGER = LogManager.getLogger();

    private final Redactor redactor;

    /**
     * Default constructor.
     *
     * @param driver the {@link RemoteWebDriver}
     */
    public RedactorImpl(final RemoteWebDriver driver) {
        redactor = switch (CONFIG.redactionType()) {
            case TEXT -> new TextRedactor(driver);
            case OVERLAY -> new OverlayRedactor(driver);
        };
    }

    @Override
    public void redact(final WebElement element, final String description) {
        logElementToBeRedacted(element);
        redactor.redact(element, description);
        LOGGER.trace("");
    }

    @Override
    public void redactPasskey(final WebElement element) {
        logElementToBeRedacted(element);
        redactor.redactPasskey(element);
        LOGGER.trace("");
    }

    @Override
    public void redactEmail(final WebElement element) {
        logElementToBeRedacted(element);
        redactor.redactEmail(element);
        LOGGER.trace("");
    }

    @Override
    public void redactIpAddress(final WebElement element) {
        logElementToBeRedacted(element);
        redactor.redactIpAddress(element);
        LOGGER.trace("");
    }

    private static void logElementToBeRedacted(final WebElement element) {
        LOGGER.info("\t\t- Found: '{}' in <{}>", NEWLINE_PATTERN.matcher(element.getText()).replaceAll(""), element.getTagName());
    }
}
