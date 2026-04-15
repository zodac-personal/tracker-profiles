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

package net.zodac.tracker.framework.progress;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * A {@link PrintStream} wrapper that keeps a Clique progress bar pinned to the bottom of the console output.
 *
 * <p>
 * Install this as {@code System.out} before execution begins. Every write is intercepted:
 *
 * <ul>
 *   <li>Bar writes (identified by a leading carriage-return {@code \r}, as produced by Clique) are passed directly to the underlying
 *       stream so the bar overwrites itself in place.</li>
 *   <li>All other (log) writes first clear the current terminal line with {@code \r\u001B[2K} to remove any progress bar that
 *       is displayed there, then write the log content. After any write that ends with a newline {@code \n}, the progress bar is
 *       re-rendered via {@link #renderBar()}.</li>
 * </ul>
 *
 * <p>
 * This class is not thread-safe and is designed for single-threaded console output.
 */
public final class ProgressBarPrintStream extends PrintStream {

    private static final byte[] CLEAR_LINE = "\r\u001B[2K".getBytes(StandardCharsets.UTF_8);
    private static final PrintStream WRAPPED_STREAM = System.out;

    private final ProgressBarManager progressBarManager;

    /**
     * Creates a {@link ProgressBarPrintStream} that wraps {@link System#out}.
     */
    public ProgressBarPrintStream(final ProgressBarManager progressBarManager) {
        super(System.out, true, StandardCharsets.UTF_8);
        this.progressBarManager = progressBarManager;
    }

    @Override
    public void write(final int b) {
        write(new byte[] {(byte) b}, 0, 1);
    }

    @Override
    public void write(final byte[] buf, final int off, final int len) {
        if (len == 0) {
            return;
        }

        final boolean isBarWrite = buf[off] == '\r';
        if (!isBarWrite && progressBarManager.isActive()) {
            WRAPPED_STREAM.write(CLEAR_LINE, 0, CLEAR_LINE.length);
        }

        WRAPPED_STREAM.write(buf, off, len);

        if (!isBarWrite && buf[off + len - 1] == '\n' && progressBarManager.isActive()) {
            renderBar();
        }
    }

    private void renderBar() {
        final byte[] barBytes = ("\r" + progressBarManager.getProgressBarContent()).getBytes(StandardCharsets.UTF_8);
        WRAPPED_STREAM.write(barBytes, 0, barBytes.length);
        WRAPPED_STREAM.flush();
    }

    @Override
    public void close() {
        // super.close(); // Don't call super.close() or else it will close System.out and prevent any further logging
        // Clique appends a newline after the 100% render, leaving the cursor on the line below the bar
        // Move up one line `ESC[1A` before erasing so the bar line itself is cleared
        final byte[] clearBytes = "\u001B[1A\r\u001B[2K".getBytes(StandardCharsets.UTF_8);
        WRAPPED_STREAM.write(clearBytes, 0, clearBytes.length);
        WRAPPED_STREAM.flush();
    }
}
