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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Implementation of {@link Redactor} that redacts text by applying a Gaussian blur directly to the sensitive content within the impacted
 * {@link WebElement}.
 */
class BlurRedactor implements Redactor {

    private static final String BLUR_DEFINITION = "blur(0.5em)";
    private static final String IRC_KEY_PREFIX_ALTERNATION = "IRC Key";
    private static final String TORRENT_PASSKEY_PREFIX_ALTERNATION = "Passkey|Pass Key";

    private final RemoteWebDriver driver;

    /**
     * Default constructor.
     *
     * @param driver the {@link RemoteWebDriver}
     */
    BlurRedactor(final RemoteWebDriver driver) {
        this.driver = driver;
    }

    @Override
    public int redact(final WebElement element, final String description, final RedactionBuffer buffer) {
        driver.executeScript(String.format("arguments[0].style.filter = '%s'", BLUR_DEFINITION), element);
        return 1;
    }

    @Override
    public int redactEmail(final WebElement element, final RedactionBuffer buffer) {
        final String script = """
            var element = arguments[0]
            var blur_definition = '%s'
            var email_regex = /[a-zA-Z0-9._+\\-*]+@[a-zA-Z0-9.\\-*]+\\.[a-zA-Z*]{2,}/g
            var counter = 0

            // Use TreeWalker to process only text nodes, so attribute values are never modified
            var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false)
            var text_nodes = []
            var node
            while (node = walker.nextNode()) {
              text_nodes.push(node)
            }

            for (var t = 0; t < text_nodes.length; t++) {
              var text_node = text_nodes[t]
              var text = text_node.textContent
              email_regex.lastIndex = 0
              if (!email_regex.test(text)) continue
              email_regex.lastIndex = 0
              var fragment = document.createDocumentFragment()
              var last_index = 0
              var match
              while ((match = email_regex.exec(text)) !== null) {
                if (match.index > last_index) {
                  fragment.appendChild(document.createTextNode(text.slice(last_index, match.index)))
                }
                var span = document.createElement('span')
                span.className = 'redact-target'
                span.dataset.index = counter++
                span.textContent = match[0]
                fragment.appendChild(span)
                last_index = match.index + match[0].length
              }
              if (last_index < text.length) {
                fragment.appendChild(document.createTextNode(text.slice(last_index)))
              }
              text_node.parentNode.replaceChild(fragment, text_node)
            }

            // Apply blur directly to each matched span
            var target_spans = element.querySelectorAll('.redact-target')
            for (var i = 0; i < target_spans.length; i++) {
              target_spans[i].style.filter = blur_definition
            }

            // Also check 'value' attributes on the element and its descendants
            var value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
            for (var e = 0; e < value_elements.length; e++) {
              var val = value_elements[e].getAttribute('value')
              email_regex.lastIndex = 0
              if (val && email_regex.test(val)) {
                value_elements[e].style.filter = blur_definition
              }
            }
            """.formatted(BLUR_DEFINITION);

        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactIpAddress(final WebElement element, final RedactionBuffer buffer) {
        final String script = """
            var element = arguments[0]
            var blur_definition = '%s'

            var ipv4_regex = /((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)/g
            var ipv4_masked_regex = /((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){2}x\\.x/g
            var ipv6_full_regex = /([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}/g
            var ipv6_partial_regex = /([0-9a-fA-F]{4}:){3,7}[0-9a-fA-F]{0,4}/g
            var counter = 0

            function wrap_matches_in_text_node(text_node, regex) {
              var text = text_node.textContent
              regex.lastIndex = 0
              if (!regex.test(text)) return
              regex.lastIndex = 0
              var fragment = document.createDocumentFragment()
              var last_index = 0
              var match
              while ((match = regex.exec(text)) !== null) {
                if (match.index > last_index) {
                  fragment.appendChild(document.createTextNode(text.slice(last_index, match.index)))
                }
                var span = document.createElement('span')
                span.className = 'redact-target'
                span.dataset.index = counter++
                span.textContent = match[0]
                fragment.appendChild(span)
                last_index = match.index + match[0].length
              }
              if (last_index < text.length) {
                fragment.appendChild(document.createTextNode(text.slice(last_index)))
              }
              text_node.parentNode.replaceChild(fragment, text_node)
            }

            // Use TreeWalker to process only text nodes, so attribute values are never modified
            // Order matters: check masked IPv4 before regular IPv4 to avoid partial matches
            function apply_regex(regex) {
              var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false)
              var text_nodes = []
              var node
              while (node = walker.nextNode()) {
                text_nodes.push(node)
              }
              for (var t = 0; t < text_nodes.length; t++) {
                wrap_matches_in_text_node(text_nodes[t], regex)
              }
            }

            apply_regex(ipv4_masked_regex)
            apply_regex(ipv6_full_regex)
            apply_regex(ipv6_partial_regex)
            apply_regex(ipv4_regex)

            // Apply blur directly to each matched span
            var target_spans = element.querySelectorAll('.redact-target')
            for (var i = 0; i < target_spans.length; i++) {
              target_spans[i].style.filter = blur_definition
            }

            // Also check 'value' attributes on the element and its descendants
            var value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
            for (var e = 0; e < value_elements.length; e++) {
              var val = value_elements[e].getAttribute('value')
              ipv4_masked_regex.lastIndex = 0
              ipv6_full_regex.lastIndex = 0
              ipv6_partial_regex.lastIndex = 0
              ipv4_regex.lastIndex = 0
              if (val && (ipv4_masked_regex.test(val) || ipv6_full_regex.test(val) || ipv6_partial_regex.test(val) || ipv4_regex.test(val))) {
                value_elements[e].style.filter = blur_definition
              }
            }
            """.formatted(BLUR_DEFINITION);

        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactIrcPasskey(final WebElement element, final RedactionBuffer buffer) {
        final String script = """
            var element = arguments[0]
            var full_text = element.textContent
            var prefix_regex = /^\\s*(%1$s)\\s*:\\s*/i
            var prefix_match = prefix_regex.exec(full_text)

            if (!prefix_match) {
              element.style.filter = '%2$s'
              return
            }

            var prefix_len = prefix_match[0].length

            // Collect all text nodes first to avoid TreeWalker/DOM mutation issues
            var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false)
            var text_nodes = []
            var node
            while (node = walker.nextNode()) {
              text_nodes.push(node)
            }

            var chars_seen = 0
            var past_prefix = false
            for (var i = 0; i < text_nodes.length; i++) {
              var tn = text_nodes[i]
              var tn_text = tn.textContent
              var tn_len = tn_text.length
              if (past_prefix) {
                var span = document.createElement('span')
                span.textContent = tn_text
                span.style.filter = '%2$s'
                tn.parentNode.replaceChild(span, tn)
              } else if (chars_seen + tn_len <= prefix_len) {
                chars_seen += tn_len
              } else {
                var split_pos = prefix_len - chars_seen
                var parent = tn.parentNode
                if (split_pos > 0) {
                  parent.insertBefore(document.createTextNode(tn_text.slice(0, split_pos)), tn)
                }
                var sensitive_span = document.createElement('span')
                sensitive_span.textContent = tn_text.slice(split_pos)
                sensitive_span.style.filter = '%2$s'
                parent.replaceChild(sensitive_span, tn)
                past_prefix = true
              }
            }

            // Also check 'value' attributes on the element and its descendants
            var value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
            for (var e = 0; e < value_elements.length; e++) {
              var val = value_elements[e].getAttribute('value')
              if (val) {
                value_elements[e].style.filter = '%2$s'
              }
            }
            """.formatted(IRC_KEY_PREFIX_ALTERNATION, BLUR_DEFINITION);

        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactTorrentPasskey(final WebElement element, final RedactionBuffer buffer) {
        final String script = """
            var element = arguments[0]
            var full_text = element.textContent
            var prefix_regex = /^\\s*(%1$s)\\s*:\\s*/i
            var prefix_match = prefix_regex.exec(full_text)

            if (!prefix_match) {
              element.style.filter = '%2$s'
              return
            }

            var prefix_len = prefix_match[0].length

            // Collect all text nodes first to avoid TreeWalker/DOM mutation issues
            var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false)
            var text_nodes = []
            var node
            while (node = walker.nextNode()) {
              text_nodes.push(node)
            }

            var chars_seen = 0
            var past_prefix = false
            for (var i = 0; i < text_nodes.length; i++) {
              var tn = text_nodes[i]
              var tn_text = tn.textContent
              var tn_len = tn_text.length
              if (past_prefix) {
                var span = document.createElement('span')
                span.textContent = tn_text
                span.style.filter = '%2$s'
                tn.parentNode.replaceChild(span, tn)
              } else if (chars_seen + tn_len <= prefix_len) {
                chars_seen += tn_len
              } else {
                var split_pos = prefix_len - chars_seen
                var parent = tn.parentNode
                if (split_pos > 0) {
                  parent.insertBefore(document.createTextNode(tn_text.slice(0, split_pos)), tn)
                }
                var sensitive_span = document.createElement('span')
                sensitive_span.textContent = tn_text.slice(split_pos)
                sensitive_span.style.filter = '%2$s'
                parent.replaceChild(sensitive_span, tn)
                past_prefix = true
              }
            }

            // Also check 'value' attributes on the element and its descendants
            var value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
            for (var e = 0; e < value_elements.length; e++) {
              var val = value_elements[e].getAttribute('value')
              if (val) {
                value_elements[e].style.filter = '%2$s'
              }
            }
            """.formatted(TORRENT_PASSKEY_PREFIX_ALTERNATION, BLUR_DEFINITION);

        driver.executeScript(script, element);
        return 1;
    }
}
