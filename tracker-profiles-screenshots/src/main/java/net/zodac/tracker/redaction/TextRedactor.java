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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link Redactor} that redacts text by replacing the sensitive information with some placeholder text in the impacted
 * {@link WebElement}.
 */
class TextRedactor implements Redactor {

    protected static final String NON_BREAKING_SPACE = "\u2002";
    protected static final String IMG_TAG_NAME = "img";
    protected static final String CALL_IP_SCRIPT = "window.__textRedactIpAddress.apply(null, arguments);";
    protected static final String INSTALL_IP_SCRIPT = Redactor.loadScript("text_redact_ip_address.js");

    private static final ApplicationConfiguration CONFIG = Configuration.get();
    private static final Pattern IRC_KEY_PREFIX = Pattern.compile("^\\s*(IRC Key)\\s*:\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TORRENT_PASSKEY_PREFIX = Pattern.compile("^\\s*(Passkey|Pass Key)\\s*:\\s*", Pattern.CASE_INSENSITIVE);
    private static final Logger LOGGER = LogManager.getLogger();

    protected final RemoteWebDriver driver;
    private final String redactionText;

    /**
     * Default constructor.
     *
     * @param driver the {@link RemoteWebDriver}
     */
    protected TextRedactor(final RemoteWebDriver driver, final String redactionText) {
        this.driver = driver;
        this.redactionText = redactionText;
    }

    /**
     * Creates a {@link TextRedactor}.
     *
     * @param driver the {@link RemoteWebDriver}
     * @return the created {@link TextRedactor}
     */
    static TextRedactor create(final RemoteWebDriver driver) {
        final TextRedactor redactor = new TextRedactor(driver, CONFIG.redactionText());
        driver.executeScript(INSTALL_IP_SCRIPT);
        return redactor;
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
                "<span style=\"display:inline-flex;align-items:center;justify-content:center;width:%dpx;height:%dpx\">%s</span>"
                    .formatted(width, height, redactionText);
            setOuterHtml(element, span);
            return 1;
        }
        setInnerText(element, "%s: %s".formatted(description, redactionText));
        return 1;
    }

    @Override
    public int redactEmail(final WebElement element, final RedactionBuffer buffer) {
        final String htmlContent = retrieveOuterHtml(element);
        setOuterHtml(element, EMAIL.matcher(htmlContent).replaceAll(match -> replacement(match.group().length())));
        return 1;
    }

    @Override
    public int redactIpAddress(final WebElement element, final RedactionBuffer buffer) {
        driver.executeScript(CALL_IP_SCRIPT, element, redactionText);
        return 1;
    }

    @Override
    public int redactIrcPasskey(final WebElement element, final RedactionBuffer buffer) {
        final String originalText = element.getText();
        final String prefix = getPrefixFromMatcher(element, IRC_KEY_PREFIX.matcher(originalText));
        setInnerText(element, prefix + replacement(originalText.length() - prefix.length()));
        return 1;
    }

    @Override
    public int redactTorrentPasskey(final WebElement element, final RedactionBuffer buffer) {
        final String originalText = element.getText();
        final String prefix = getPrefixFromMatcher(element, TORRENT_PASSKEY_PREFIX.matcher(originalText));
        setInnerText(element, prefix + replacement(originalText.length() - prefix.length()));
        return 1;
    }

    /**
     * Returns the replacement string for the given number of characters. Subclasses may override to change what is substituted for matched text.
     *
     * @param length the number of characters in the original text
     * @return the replacement string
     */
    protected String replacement(final int length) {
        if (redactionText.length() >= length) {
            return redactionText.substring(0, length);
        }
        return redactionText + NON_BREAKING_SPACE.repeat(length - redactionText.length());
    }

    /**
     * Sets the {@code innerText} of the given {@link WebElement}.
     *
     * @param element the target element
     * @param text    the text to set
     */
    protected void setInnerText(final WebElement element, final String text) {
        driver.executeScript("arguments[0].innerText = arguments[1];", element, text);
    }

    /**
     * Replaces the {@code outerHTML} of the given {@link WebElement}.
     *
     * @param element     the target element
     * @param htmlContent the HTML to set
     */
    protected void setOuterHtml(final WebElement element, final String htmlContent) {
        driver.executeScript("arguments[0].outerHTML = arguments[1];", element, htmlContent);
    }

    /**
     * Returns the {@code offsetWidth} of the given {@link WebElement} in pixels.
     *
     * @param element the target element
     * @return the width in pixels, or {@code 0} if unavailable
     */
    protected long getOffsetWidth(final WebElement element) {
        final Object offset = driver.executeScript("return arguments[0].offsetWidth;", element);
        return offset instanceof final Number n ? n.longValue() : 0L;
    }

    /**
     * Returns the {@code offsetHeight} of the given {@link WebElement} in pixels.
     *
     * @param element the target element
     * @return the height in pixels, or {@code 0} if unavailable
     */
    protected long getOffsetHeight(final WebElement element) {
        final Object height = driver.executeScript("return arguments[0].offsetHeight;", element);
        return height instanceof final Number n ? n.longValue() : 0L;
    }

    private String retrieveOuterHtml(final WebElement element) {
        final String htmlContent = (String) driver.executeScript("return arguments[0].outerHTML", element);
        if (htmlContent == null) {
            LOGGER.trace("Found no outerHTML in {}", element);
            return "";
        }
        return htmlContent;
    }

    private static String getPrefixFromMatcher(final WebElement element, final Matcher matcher) {
        return matcher.find() ? element.getText().substring(0, matcher.end()) : "";
    }
}
