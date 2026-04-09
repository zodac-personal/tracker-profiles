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
import static net.zodac.tracker.util.TextSearcher.IPV6_PARTIAL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link Redactor} that redacts text by replacing the sensitive information with some placeholder text in the impacted
 * {@link WebElement}.
 */
class TextRedactor implements Redactor {

    private static final String DEFAULT_REDACTION_TEXT = "----";
    private static final Pattern IRC_KEY_PREFIX = Pattern.compile("^\\s*(IRC Key)\\s*:\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TORRENT_PASSKEY_PREFIX = Pattern.compile("^\\s*(Passkey|Pass Key)\\s*:\\s*", Pattern.CASE_INSENSITIVE);
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
    public int redact(final WebElement element, final String description, final RedactionBuffer buffer) {
        final String redactionText = String.format("%s: %s", description, DEFAULT_REDACTION_TEXT);
        setInnerText(element, redactionText);
        return 1;
    }

    @Override
    public int redactEmail(final WebElement element, final RedactionBuffer buffer) {
        final String htmlContent = retrieveOuterHtml(element);
        final String substitutionHtmlContent = replaceEmail(htmlContent);
        setOuterHtml(element, substitutionHtmlContent);
        return 1;
    }

    @Override
    public int redactIpAddress(final WebElement element, final RedactionBuffer buffer) {
        final String htmlContent = retrieveOuterHtml(element);
        final String substitutionHtmlContent = replaceIpAddresses(htmlContent);
        setOuterHtml(element, substitutionHtmlContent);
        return 1;
    }

    @Override
    public int redactIrcPasskey(final WebElement element, final RedactionBuffer buffer) {
        final Matcher matcher = IRC_KEY_PREFIX.matcher(element.getText());
        final String prefix = getPrefixFromMatcher(element, matcher);
        setInnerText(element, prefix + DEFAULT_REDACTION_TEXT);
        return 1;
    }

    @Override
    public int redactTorrentPasskey(final WebElement element, final RedactionBuffer buffer) {
        final Matcher matcher = TORRENT_PASSKEY_PREFIX.matcher(element.getText());
        final String prefix = getPrefixFromMatcher(element, matcher);
        setInnerText(element, prefix + DEFAULT_REDACTION_TEXT);
        return 1;
    }

    private void setInnerText(final WebElement element, final String text) {
        driver.executeScript("arguments[0].innerText = arguments[1];", element, text);
    }

    private static String getPrefixFromMatcher(final WebElement element, final Matcher matcher) {
        return matcher.find() ? element.getText().substring(0, matcher.end()) : "";
    }

    private String retrieveOuterHtml(final WebElement element) {
        final String htmlContent = (String) driver.executeScript("return arguments[0].outerHTML", element);
        if (htmlContent == null) {
            LOGGER.trace("Found no outerHTML in {}", element);
            return "";
        }
        return htmlContent;
    }

    private void setOuterHtml(final WebElement element, final String htmlContent) {
        driver.executeScript("arguments[0].outerHTML = arguments[1];", element, htmlContent);
    }

    private static String replaceEmail(final String input) {
        return EMAIL.matcher(input).replaceAll(DEFAULT_REDACTION_TEXT);
    }

    private static String replaceIpAddresses(final String input) {
        return IPV4.matcher(input).replaceAll(DEFAULT_REDACTION_TEXT)
            .replaceAll(IPV4_MASKED.pattern(), DEFAULT_REDACTION_TEXT)
            .replaceAll(IPV6.pattern(), DEFAULT_REDACTION_TEXT)
            .replaceAll(IPV6_PARTIAL.pattern(), DEFAULT_REDACTION_TEXT);
    }

}
