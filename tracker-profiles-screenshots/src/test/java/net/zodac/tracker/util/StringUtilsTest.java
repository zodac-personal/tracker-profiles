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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link StringUtils}.
 */
class StringUtilsTest {

    @ParameterizedTest(name = "pluralise({0}) -> {1}")
    @MethodSource("intProvider")
    void testPluraliseByInt(final int count, final String expected) {
        assertThat(StringUtils.pluralise(count))
            .isEqualTo(expected);
    }

    private static Stream<Arguments> intProvider() {
        return Stream.of(
            arguments(1, ""),
            arguments(0, "s"),
            arguments(2, "s"),
            arguments(-1, "s"),
            arguments(100, "s")
        );
    }

    @ParameterizedTest(name = "pluralise({0}) -> {1}")
    @MethodSource("collectionProvider")
    void testPluraliseByCollection(final Collection<?> collection, final String expected) {
        assertThat(StringUtils.pluralise(collection))
            .isEqualTo(expected);
    }

    private static Stream<Arguments> collectionProvider() {
        return Stream.of(
            arguments(List.of(), "s"),
            arguments(List.of("a"), ""),
            arguments(List.of("a", "b"), "s"),
            arguments(List.of(1, 2, 3), "s"),
            arguments(Set.of("a"), ""),
            arguments(Set.of("a", "b"), "s")
        );
    }
}
