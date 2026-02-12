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

package net.zodac.tracker.util;

import java.util.regex.Pattern;

/**
 * Utility class to define {@link Pattern}s and methods to find and/or replace matches in {@link String}s.
 */
public final class TextSearcher {

    /**
     * Regex pattern to find an email substring.
     */
    public static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+\\-*]+@[a-zA-Z0-9.\\-*]+\\.[a-zA-Z*]{2,}");

    /**
     * Regex pattern to find an IPv4 address substring.
     */
    public static final Pattern IPV4 = Pattern.compile("((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)");

    /**
     * Regex pattern to find an IPv4 address that has been masked ({@code 192.168.x.x}) substring.
     */
    public static final Pattern IPV4_MASKED = Pattern.compile("((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){2}x\\.x");

    /**
     * Regex pattern to find an IPv6 address substring.
     */
    public static final Pattern IPV6 = Pattern.compile("([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}");

    private TextSearcher() {

    }

    /**
     * Checks if the {@link String} contains an email address.
     *
     * @param input the {@link String} to check
     * @return {@code true} if it contains an email address
     */
    public static boolean containsEmailAddress(final String input) {
        return EMAIL.matcher(input).find();
    }

    /**
     * Checks if the {@link String} contains an IPv4 or IPv6 address to. Also checks for a match of the first 2 octets of the IPv4 address for
     * trackers that post a partial IP address.
     *
     * @param input the {@link String} to check
     * @return {@code true} if it contains an IP address
     */
    public static boolean containsIpAddress(final String input) {
        return IPV4.matcher(input).find()
            || IPV4_MASKED.matcher(input).find()
            || IPV6.matcher(input).find();
    }
}
