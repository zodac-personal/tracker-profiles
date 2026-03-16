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
 * Implementation of {@link Redactor} that redacts text by overlaying a solid, coloured box over the impacted {@link WebElement}.
 */
class OverlayRedactor implements Redactor {

    private final RemoteWebDriver driver;

    /**
     * Default constructor.
     *
     * @param driver the {@link RemoteWebDriver}
     */
    OverlayRedactor(final RemoteWebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void redact(final WebElement element, final String description, final OverlayBuffer buffer) {
        redactElement(element, "orange", description, buffer);
    }

    @Override
    public void redactPasskey(final WebElement element, final OverlayBuffer buffer) {
        redactElement(element, "red", "Passkey", buffer);
    }

    @Override
    public void redactEmail(final WebElement element, final OverlayBuffer buffer) {
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
    }

    @Override
    public void redactIpAddress(final WebElement element, final OverlayBuffer buffer) {
        final String script = """
            var element = arguments[0]

            var ipv4_regex = /((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)/g
            var ipv4_masked_regex = /((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){2}x\\.x/g
            var ipv6_regex = /([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}/g
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
    }

    private void redactElement(final WebElement element, final String overlayColour, final String description,
                               final OverlayBuffer buffer) {
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
            overlayColour, "white", description);

        driver.executeScript(script, element);
    }
}
