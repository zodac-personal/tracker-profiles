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

import static net.zodac.tracker.util.TextSearcher.EMAIL;
import static net.zodac.tracker.util.TextSearcher.IPV4;
import static net.zodac.tracker.util.TextSearcher.IPV4_MASKED;
import static net.zodac.tracker.util.TextSearcher.IPV6;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link Redactor} that redacts text by replacing the sensitive information with some placeholder text in the impacted
 * {@link WebElement}.
 */
class TextRedactor implements Redactor {

    private static final String DEFAULT_REDACTION_TEXT = "----";
    private static final Logger LOGGER = LogManager.getLogger();

    private final RemoteWebDriver driver;

    /**
     * Default constructor.
     *
     * @param driver the {@link RemoteWebDriver}
     */
    TextRedactor(final RemoteWebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void redactPasskey(final WebElement element) {
        redactPasskey(element, null);
    }

    @Override
    public void redactPasskey(final WebElement element, final @Nullable String replacementTextPrefix) {
        final String replacementText = replacementTextPrefix == null ? DEFAULT_REDACTION_TEXT : replacementTextPrefix + DEFAULT_REDACTION_TEXT;
        driver.executeScript(String.format("arguments[0].innerText = '%s'", replacementText), element);
    }

    @Override
    public void redactEmail(final WebElement element) {
        final String htmlContent = retrieveOuterHtml(element);
        final String substitutionText = replaceEmail(htmlContent);
        setOuterHtml(element, substitutionText);
    }

    @Override
    public void redactIpAddress(final WebElement element) {
        final String htmlContent = retrieveOuterHtml(element);
        final String substitutionText = replaceIpAddresses(htmlContent);
        setOuterHtml(element, substitutionText);
    }

    private String retrieveOuterHtml(final WebElement element) {
        final String htmlContent = (String) driver.executeScript("return arguments[0].outerHTML", element);
        if (htmlContent == null) {
            LOGGER.trace("Found no outerHTML in {}", element);
            return "";
        }
        return escapeForJavaScriptString(htmlContent);
    }

    private void setOuterHtml(final WebElement element, final String htmlContent) {
        driver.executeScript(String.format("arguments[0].outerHTML = '%s'", htmlContent), element);
    }

    private static String replaceEmail(final String input) {
        return EMAIL.matcher(input).replaceAll(DEFAULT_REDACTION_TEXT);
    }

    private static String replaceIpAddresses(final String input) {
        return IPV4.matcher(input).replaceAll(DEFAULT_REDACTION_TEXT)
            .replaceAll(IPV4_MASKED.pattern(), DEFAULT_REDACTION_TEXT)
            .replaceAll(IPV6.pattern(), DEFAULT_REDACTION_TEXT);
    }

    private static String escapeForJavaScriptString(final String input) {
        final String escapedString = input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n");
        LOGGER.trace("Escaped input '{}' to '{}'", input, escapedString);
        return escapedString;
    }
}
