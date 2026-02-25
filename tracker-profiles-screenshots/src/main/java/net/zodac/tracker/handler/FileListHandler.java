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

package net.zodac.tracker.handler;

import static net.zodac.tracker.framework.xpath.HtmlElement.a;
import static net.zodac.tracker.framework.xpath.HtmlElement.div;
import static net.zodac.tracker.framework.xpath.HtmlElement.input;
import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;

import java.time.Duration;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import net.zodac.tracker.util.ScriptExecutor;
import org.openqa.selenium.By;

/**
 * Implementation of {@link AbstractTrackerHandler} for the {@code FileList} tracker.
 */
@TrackerHandler(name = "FileList", url = {
    "https://filelist.io/",
    "https://thefl.org/"
})
public class FileListHandler extends AbstractTrackerHandler {

    @Override
    protected By loginButtonSelector() {
        return XpathBuilder
            .from(div, withClass("login-btn"))
            .child(input, atIndex(1))
            .build();
    }

    @Override
    protected By postLoginSelector() {
        ScriptExecutor.explicitWait(Duration.ofSeconds(2L), "page content to load");
        return By.id("maincolumn");
    }

    @Override
    protected By profilePageSelector() {
        return XpathBuilder
            .from(div, withClass("status_avatar"))
            .child(a, atIndex(1))
            .build();
    }

    @Override
    public boolean hasSensitiveInformation() {
        return false;
    }

    @Override
    protected By logoutButtonSelector() {
        return XpathBuilder
            .from(div, withClass("statusbar"))
            .child(div, atIndex(2))
            .child(div, atIndex(3))
            .child(span, atIndex(4))
            .child(a, atIndex(1))
            .build();
    }
}
