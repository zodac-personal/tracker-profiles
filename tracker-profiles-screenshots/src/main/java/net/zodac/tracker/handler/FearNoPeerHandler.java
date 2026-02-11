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

import static net.zodac.tracker.framework.xpath.HtmlElement.span;
import static net.zodac.tracker.framework.xpath.HtmlElement.table;
import static net.zodac.tracker.framework.xpath.HtmlElement.tbody;
import static net.zodac.tracker.framework.xpath.HtmlElement.td;
import static net.zodac.tracker.framework.xpath.HtmlElement.tr;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.atIndex;
import static net.zodac.tracker.framework.xpath.XpathAttributePredicate.withClass;

import java.util.Collection;
import java.util.List;
import net.zodac.tracker.framework.annotation.TrackerHandler;
import net.zodac.tracker.framework.xpath.XpathBuilder;
import org.openqa.selenium.By;

/**
 * Extension of the {@link Unit3dHandler} for the {@code FearNoPeer} tracker.
 */
@TrackerHandler(name = "FearNoPeer", url = "https://fearnopeer.com/login") // URL set to login page to bypass Cloudflare verification
public class FearNoPeerHandler extends Unit3dHandler {

    /**
     * {@inheritDoc}
     *
     * <p>
     * For {@link FearNoPeerHandler}, the email is actually defined in a separate section, unlike other {@code UNIT3D}-based trackers. And the IP
     * address table has a different class.
     *
     * @return the {@link By} selectors for {@link FearNoPeerHandler} email IP addresses
     */
    @Override
    public Collection<By> getElementsPotentiallyContainingSensitiveInformation() {
        return List.of(
            // Email
            XpathBuilder
                .from(span, withClass("profile-hero-2026__info-value--email"))
                .build(),
            // IP address, potentially multiple entries
            XpathBuilder
                .from(table, withClass("profile-table-2026"))
                .child(tbody)
                .child(tr)
                .child(td, atIndex(2))
                .build()
        );
    }
}
