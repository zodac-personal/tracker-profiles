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

    private static final String DEFAULT_REDACTION_TEXT = "----";  // TODO: Make user configurable, careful with padding if too long
    private static final String NON_BREAKING_SPACE = "\u2002";
    private static final String IMG_TAG_NAME = "img";
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the {@link WebElement} is an {@code <img>} element, it is replaced entirely with a {@code span} of the same dimensions as the original
     * image, with the redaction text centred both horizontally and vertically. This is necessary because {@code innerText} assignment has no effect
     * on image elements.
     */
    @Override
    public int redact(final WebElement element, final String description, final RedactionBuffer buffer) {
        if (IMG_TAG_NAME.equalsIgnoreCase(element.getTagName())) {
            final long width = getOffsetWidth(element);
            final long height = getOffsetHeight(element);
            final String span =
                "<span style=\"display:inline-flex; align-items:center; justify-content:center; width:%dpx; height:%dpx \">%s</span>".formatted(width,
                    height, DEFAULT_REDACTION_TEXT);
            setOuterHtml(element, span);
            return 1;
        }
        final String redactionText = "%s: %s".formatted(description, DEFAULT_REDACTION_TEXT);
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
        final String originalText = element.getText();
        final Matcher matcher = IRC_KEY_PREFIX.matcher(originalText);
        final String prefix = getPrefixFromMatcher(element, matcher);
        setInnerText(element, prefix + paddedRedaction(originalText.length() - prefix.length()));
        return 1;
    }

    @Override
    public int redactTorrentPasskey(final WebElement element, final RedactionBuffer buffer) {
        final String originalText = element.getText();
        final Matcher matcher = TORRENT_PASSKEY_PREFIX.matcher(originalText);
        final String prefix = getPrefixFromMatcher(element, matcher);
        setInnerText(element, prefix + paddedRedaction(originalText.length() - prefix.length()));
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

    private long getOffsetWidth(final WebElement element) {
        final Object offset = driver.executeScript("return arguments[0].offsetWidth;", element);
        return offset instanceof final Number n ? n.longValue() : 0L;
    }

    private long getOffsetHeight(final WebElement element) {
        final Object height = driver.executeScript("return arguments[0].offsetHeight;", element);
        return height instanceof final Number n ? n.longValue() : 0L;
    }

    private static String replaceEmail(final String input) {
        return EMAIL.matcher(input).replaceAll(match -> paddedRedaction(match.group().length()));
    }

    private static String replaceIpAddresses(final String input) {
        final String afterIpv4 = IPV4.matcher(input).replaceAll(match -> paddedRedaction(match.group().length()));
        final String afterIpv4Masked = IPV4_MASKED.matcher(afterIpv4).replaceAll(match -> paddedRedaction(match.group().length()));
        final String afterIpv6 = IPV6.matcher(afterIpv4Masked).replaceAll(match -> paddedRedaction(match.group().length()));
        return IPV6_PARTIAL.matcher(afterIpv6).replaceAll(match -> paddedRedaction(match.group().length()));
    }

    private static String paddedRedaction(final int originalLength) {
        return DEFAULT_REDACTION_TEXT + NON_BREAKING_SPACE.repeat(Math.max(0, originalLength - DEFAULT_REDACTION_TEXT.length()));
    }
}
