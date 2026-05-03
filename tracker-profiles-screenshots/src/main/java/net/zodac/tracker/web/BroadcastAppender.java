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

package net.zodac.tracker.web;

import java.io.Serializable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Log4j2 appender installed programmatically at startup to forward formatted log events to
 * {@link LogBroadcaster} for SSE streaming to connected browsers.
 *
 * <p>
 * The pattern intentionally omits colour codes so the plain text is readable in HTML.
 */
public final class BroadcastAppender extends AbstractAppender {

    private static final String APPENDER_NAME = "BroadcastAppender";
    private static final String LOG_PATTERN = "%d{HH:mm:ss.SSS} [%-5level] %X{tracker}%msg%n";

    private BroadcastAppender(final PatternLayout layout) {
        super(APPENDER_NAME, null, layout, true, new Property[0]);
    }

    @Override
    public void append(final LogEvent event) {
        final Layout<?> layout = getLayout();
        if (layout == null) {
            return;
        }
        final Serializable serialized = layout.toSerializable(event);
        if (serialized != null) {
            LogBroadcaster.broadcast(serialized.toString());
        }
    }

    /**
     * Installs this appender into the Log4j2 root logger at {@link Level#INFO} and above.
     * Must be called once at application startup before any logging occurs.
     */
    public static void install() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        final PatternLayout layout = PatternLayout.newBuilder()
            .withPattern(LOG_PATTERN)
            .build();
        final BroadcastAppender appender = new BroadcastAppender(layout);
        appender.start();
        config.addAppender(appender);
        config.getRootLogger().addAppender(appender, Level.INFO, null);
        ctx.updateLoggers();
    }
}
