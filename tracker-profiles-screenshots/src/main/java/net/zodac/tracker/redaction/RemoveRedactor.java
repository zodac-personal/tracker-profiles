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

import net.zodac.tracker.framework.config.ApplicationConfiguration;
import net.zodac.tracker.framework.config.Configuration;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Extension of {@link TextRedactor} that redacts text by replacing the sensitive information with non-breaking spaces of the same length, preserving
 * the page layout.
 */
final class RemoveRedactor extends TextRedactor {

    private static final ApplicationConfiguration CONFIG = Configuration.get();

    private RemoveRedactor(final RemoteWebDriver driver) {
        super(driver, CONFIG.redactionText());
    }

    /**
     * Creates a {@link RemoveRedactor}.
     *
     * @param driver the {@link RemoteWebDriver}
     * @return the created {@link RemoveRedactor}
     */
    static RemoveRedactor create(final RemoteWebDriver driver) {
        final RemoveRedactor redactor = new RemoveRedactor(driver);
        driver.executeScript(INSTALL_IP_SCRIPT);
        return redactor;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the {@link WebElement} is an {@code <img>} element, it is replaced entirely with a blank {@code span} of the same dimensions as the original
     * image. This is necessary because {@code innerText} assignment has no effect on image elements.
     */
    @Override
    public int redact(final WebElement element, final String description, final RedactionBuffer buffer) {
        if (IMG_TAG_NAME.equalsIgnoreCase(element.getTagName())) {
            final long width = getOffsetWidth(element);
            final long height = getOffsetHeight(element);
            setOuterHtml(element, "<span style=\"display:inline-flex;width:%dpx;height:%dpx\"></span>".formatted(width, height));
            return 1;
        }
        setInnerText(element, replacement(element.getText().length()));
        return 1;
    }

    @Override
    public int redactIpAddress(final WebElement element, final RedactionBuffer buffer) {
        driver.executeScript(CALL_IP_SCRIPT, element, "");
        return 1;
    }

    @Override
    protected String replacement(final int length) {
        return NON_BREAKING_SPACE.repeat(length);
    }
}
