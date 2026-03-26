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
 * Implementation of {@link Redactor} that redacts text by covering the impacted {@link WebElement} with a solid, coloured box with a title.
 */
class BoxRedactor implements Redactor {

    private static final String IRC_KEY_PREFIX_ALTERNATION = "IRC Key";
    private static final String TORRENT_PASSKEY_PREFIX_ALTERNATION = "Passkey|Pass Key";

    private final RemoteWebDriver driver;

    /**
     * Default constructor.
     *
     * @param driver the {@link RemoteWebDriver}
     */
    BoxRedactor(final RemoteWebDriver driver) {
        this.driver = driver;
    }

    @Override
    public int redact(final WebElement element, final String description, final RedactionBuffer buffer) {
        final String script = """
            var element = arguments[0]
            var bounding_rectangle = element.getBoundingClientRect()
            var scroll_top = window.pageYOffset || document.documentElement.scrollTop
            var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
            
            // Get computed styles to check for any overhanging text
            var computed_style = window.getComputedStyle(element)
            
            var overlay = document.createElement('div')
            
            overlay.style.position = 'absolute'
            overlay.style.left = (bounding_rectangle.left + scroll_left - %d) + 'px'
            overlay.style.top = (bounding_rectangle.top + scroll_top - %d) + 'px'
            overlay.style.width = (bounding_rectangle.width + %d + %d) + 'px'
            overlay.style.height = (bounding_rectangle.height + %d + %d) + 'px'
            
            overlay.style.backgroundColor = '%s'
            overlay.style.zIndex = '9999'
            overlay.style.pointerEvents = 'none'
            overlay.style.boxSizing = 'border-box'
            document.body.appendChild(overlay)
            
            // Add white text centered on the overlay
            overlay.style.display = 'flex'
            overlay.style.alignItems = 'center'
            overlay.style.justifyContent = 'center'
            overlay.style.color = '%s'
            overlay.style.fontSize = computed_style.fontSize
            overlay.style.fontFamily = computed_style.fontFamily
            overlay.style.fontWeight = 'bold'
            overlay.textContent = '%s'
            
            overlay.id = 'redact-' + Date.now()
            return overlay.id
            """.formatted(buffer.left(), buffer.up(), buffer.left(), buffer.right(), buffer.up(), buffer.down(),
            "orange", "white", description);

        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactEmail(final WebElement element, final RedactionBuffer buffer) {
        final String script = """
            var element = arguments[0]
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
            
            // Get all redaction target spans
            var target_spans = element.querySelectorAll('.redact-target')
            var overlay_ids = []
            
            var scroll_top = window.pageYOffset || document.documentElement.scrollTop
            var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
            
            // Get computed styles to check for any overhanging text
            var computed_style = window.getComputedStyle(element)
            
            // Create overlay for each target
            for (var i = 0; i < target_spans.length; i++) {
              var bounding_rectangle = target_spans[i].getBoundingClientRect()
              var overlay = document.createElement('div')
            
              overlay.style.position = 'absolute'
              overlay.style.left = (bounding_rectangle.left + scroll_left - %d) + 'px'
              overlay.style.top = (bounding_rectangle.top + scroll_top - %d) + 'px'
              overlay.style.width = (bounding_rectangle.width + %d + %d) + 'px'
              overlay.style.height = (bounding_rectangle.height + %d + %d) + 'px'
            
              overlay.style.backgroundColor = '%s'
              overlay.style.zIndex = '9999'
              overlay.style.pointerEvents = 'none'
              overlay.style.boxSizing = 'border-box'
              overlay.id = 'redact-' + Date.now() + '-' + i
              document.body.appendChild(overlay)
              overlay_ids.push(overlay.id)
            
              // Add white text centered on the overlay
              overlay.style.display = 'flex'
              overlay.style.alignItems = 'center'
              overlay.style.justifyContent = 'center'
              overlay.style.color = '%s'
              overlay.style.fontSize = computed_style.fontSize
              overlay.style.fontFamily = computed_style.fontFamily
              overlay.style.fontWeight = 'bold'
              overlay.textContent = '%s'
            }
            
            return overlay_ids.join(',')
            """.formatted(buffer.left(), buffer.up(), buffer.left(), buffer.right(), buffer.up(), buffer.down(),
            "blue", "white", "Email");

        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactIpAddress(final WebElement element, final RedactionBuffer buffer) {
        final String script = """
            var element = arguments[0]
            
            var ipv4_regex = /((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)/g
            var ipv4_masked_regex = /((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){2}x\\.x/g
            var ipv6_regex = /([0-9a-fA-F]{4}:){3,7}[0-9a-fA-F]{0,4}/g
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
            apply_regex(ipv6_regex)
            apply_regex(ipv4_regex)
            
            // Get all redaction target spans
            var target_spans = element.querySelectorAll('.redact-target')
            var overlay_ids = []
            
            var scroll_top = window.pageYOffset || document.documentElement.scrollTop
            var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
            
            // Get computed styles to check for any overhanging text
            var computed_style = window.getComputedStyle(element)
            
            // Create overlay for each target
            for (var i = 0; i < target_spans.length; i++) {
              var bounding_rectangle = target_spans[i].getBoundingClientRect()
              var overlay = document.createElement('div')
            
              overlay.style.position = 'absolute'
              overlay.style.left = (bounding_rectangle.left + scroll_left - %d) + 'px'
              overlay.style.top = (bounding_rectangle.top + scroll_top - %d) + 'px'
              overlay.style.width = (bounding_rectangle.width + %d + %d) + 'px'
              overlay.style.height = (bounding_rectangle.height + %d + %d) + 'px'
            
              overlay.style.backgroundColor = '%s'
              overlay.style.zIndex = '9999'
              overlay.style.pointerEvents = 'none'
              overlay.style.boxSizing = 'border-box'
              overlay.id = 'redact-' + Date.now() + '-' + i
              document.body.appendChild(overlay)
              overlay_ids.push(overlay.id)
            
              // Add white text centered on the overlay
              overlay.style.display = 'flex'
              overlay.style.alignItems = 'center'
              overlay.style.justifyContent = 'center'
              overlay.style.color = '%s'
              overlay.style.fontSize = computed_style.fontSize
              overlay.style.fontFamily = computed_style.fontFamily
              overlay.style.fontWeight = 'bold'
              overlay.textContent = '%s'
            }
            
            return overlay_ids.join(',')
            """.formatted(buffer.left(), buffer.up(), buffer.left(), buffer.right(), buffer.up(), buffer.down(),
            "yellow", "black", "IP");

        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactIrcPasskey(final WebElement element, final RedactionBuffer buffer) {
        final String script = """
            var element = arguments[0]
            var full_text = element.textContent
            var prefix_regex = /^\\s*(%7$s)\\s*:\\s*/i
            var prefix_match = prefix_regex.exec(full_text)
            
            var scroll_top = window.pageYOffset || document.documentElement.scrollTop
            var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
            var computed_style = window.getComputedStyle(element)
            
            if (!prefix_match) {
              var bounding_rectangle = element.getBoundingClientRect()
              var overlay = document.createElement('div')
              overlay.style.position = 'absolute'
              overlay.style.left = (bounding_rectangle.left + scroll_left - %1$d) + 'px'
              overlay.style.top = (bounding_rectangle.top + scroll_top - %2$d) + 'px'
              overlay.style.width = (bounding_rectangle.width + %1$d + %3$d) + 'px'
              overlay.style.height = (bounding_rectangle.height + %2$d + %4$d) + 'px'
              overlay.style.backgroundColor = 'gray'
              overlay.style.zIndex = '9999'
              overlay.style.pointerEvents = 'none'
              overlay.style.boxSizing = 'border-box'
              overlay.style.display = 'flex'
              overlay.style.alignItems = 'center'
              overlay.style.justifyContent = 'center'
              overlay.style.color = 'white'
              overlay.style.fontSize = computed_style.fontSize
              overlay.style.fontFamily = computed_style.fontFamily
              overlay.style.fontWeight = 'bold'
              overlay.textContent = 'IRC'
              overlay.id = 'redact-' + Date.now()
              document.body.appendChild(overlay)
              return overlay.id
            }
            
            var prefix_len = prefix_match[0].length
            
            // Collect all text nodes first to avoid TreeWalker/DOM mutation issues
            var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false)
            var text_nodes = []
            var node
            while (node = walker.nextNode()) {
              text_nodes.push(node)
            }
            
            // Wrap sensitive portions in spans
            var chars_seen = 0
            var past_prefix = false
            var sensitive_spans = []
            for (var i = 0; i < text_nodes.length; i++) {
              var tn = text_nodes[i]
              var tn_text = tn.textContent
              var tn_len = tn_text.length
              if (past_prefix) {
                var span = document.createElement('span')
                span.textContent = tn_text
                tn.parentNode.replaceChild(span, tn)
                sensitive_spans.push(span)
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
                parent.replaceChild(sensitive_span, tn)
                sensitive_spans.push(sensitive_span)
                past_prefix = true
              }
            }
            
            // Create an overlay for each sensitive span
            var overlay_ids = []
            for (var j = 0; j < sensitive_spans.length; j++) {
              var bounding_rectangle = sensitive_spans[j].getBoundingClientRect()
              var overlay = document.createElement('div')
              overlay.style.position = 'absolute'
              overlay.style.left = (bounding_rectangle.left + scroll_left - %1$d) + 'px'
              overlay.style.top = (bounding_rectangle.top + scroll_top - %2$d) + 'px'
              overlay.style.width = (bounding_rectangle.width + %1$d + %3$d) + 'px'
              overlay.style.height = (bounding_rectangle.height + %2$d + %4$d) + 'px'
              overlay.style.backgroundColor = '%5$s'
              overlay.style.zIndex = '9999'
              overlay.style.pointerEvents = 'none'
              overlay.style.boxSizing = 'border-box'
              overlay.style.display = 'flex'
              overlay.style.alignItems = 'center'
              overlay.style.justifyContent = 'center'
              overlay.style.color = 'white'
              overlay.style.fontSize = computed_style.fontSize
              overlay.style.fontFamily = computed_style.fontFamily
              overlay.style.fontWeight = 'bold'
              if (j == 0) {
                overlay.textContent = '%6$s'
              }
              overlay.id = 'redact-' + Date.now() + '-' + j
              document.body.appendChild(overlay)
              overlay_ids.push(overlay.id)
            }
            return overlay_ids.join(',')
            """.formatted(buffer.left(), buffer.up(), buffer.right(), buffer.down(), "gray", "IRC", IRC_KEY_PREFIX_ALTERNATION);

        driver.executeScript(script, element);
        return 1;
    }

    @Override
    public int redactTorrentPasskey(final WebElement element, final RedactionBuffer buffer) {
        final String script = """
            var element = arguments[0]
            var full_text = element.textContent
            var prefix_regex = /^\\s*(%7$s)\\s*:\\s*/i
            var prefix_match = prefix_regex.exec(full_text)

            var scroll_top = window.pageYOffset || document.documentElement.scrollTop
            var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
            var computed_style = window.getComputedStyle(element)

            var rects_to_cover = []

            if (!prefix_match) {
              rects_to_cover.push(element.getBoundingClientRect())
            } else {
              var prefix_len = prefix_match[0].length

              // Collect all text nodes first to avoid TreeWalker/DOM mutation issues
              var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false)
              var text_nodes = []
              var node
              while (node = walker.nextNode()) {
                text_nodes.push(node)
              }

              // Wrap sensitive portions in spans and record their bounding rectangles
              var chars_seen = 0
              var past_prefix = false
              for (var i = 0; i < text_nodes.length; i++) {
                var tn = text_nodes[i]
                var tn_text = tn.textContent
                var tn_len = tn_text.length
                if (past_prefix) {
                  var span = document.createElement('span')
                  span.textContent = tn_text
                  tn.parentNode.replaceChild(span, tn)
                  rects_to_cover.push(span.getBoundingClientRect())
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
                  parent.replaceChild(sensitive_span, tn)
                  rects_to_cover.push(sensitive_span.getBoundingClientRect())
                  past_prefix = true
                }
              }
            }

            // Create an overlay for each rect to cover
            var overlay_ids = []
            for (var j = 0; j < rects_to_cover.length; j++) {
              var bounding_rectangle = rects_to_cover[j]
              var overlay = document.createElement('div')
              overlay.style.position = 'absolute'
              overlay.style.left = (bounding_rectangle.left + scroll_left - %1$d) + 'px'
              overlay.style.top = (bounding_rectangle.top + scroll_top - %2$d) + 'px'
              overlay.style.width = (bounding_rectangle.width + %1$d + %3$d) + 'px'
              overlay.style.height = (bounding_rectangle.height + %2$d + %4$d) + 'px'
              overlay.style.backgroundColor = '%5$s'
              overlay.style.zIndex = '9999'
              overlay.style.pointerEvents = 'none'
              overlay.style.boxSizing = 'border-box'
              overlay.style.display = 'flex'
              overlay.style.alignItems = 'center'
              overlay.style.justifyContent = 'center'
              overlay.style.color = 'white'
              overlay.style.fontSize = computed_style.fontSize
              overlay.style.fontFamily = computed_style.fontFamily
              overlay.style.fontWeight = 'bold'
              if (j == 0) {
                overlay.textContent = '%6$s'
              }
              overlay.id = 'redact-' + Date.now() + (j > 0 ? '-' + j : '')
              document.body.appendChild(overlay)
              overlay_ids.push(overlay.id)
            }
            return overlay_ids.join(',')
            """.formatted(buffer.left(), buffer.up(), buffer.right(), buffer.down(), "red", "Passkey", TORRENT_PASSKEY_PREFIX_ALTERNATION);

        driver.executeScript(script, element);
        return 1;
    }
}
