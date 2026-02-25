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
// TODO: Allow caller to define buffer bounds of box to cover things completely
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
    public void redact(final WebElement element, final String description) {
        redactElement(element, "orange", description);
    }

    @Override
    public void redactPasskey(final WebElement element) {
        redactElement(element, "red", "Passkey");
    }

    @Override
    public void redactEmail(final WebElement element) {
        final String script = """
            var element = arguments[0]
            var email_regex = /[a-zA-Z0-9._+\\-*]+@[a-zA-Z0-9.\\-*]+\\.[a-zA-Z*]{2,}/g
            
            var original_html = element.innerHTML
            var counter = 0
            
            // Replace all matching patterns with spans
            // Order matters: check masked IPv4 before regular IPv4 to avoid partial matches
            var new_html = original_html
            new_html = new_html.replace(email_regex, function(match) {
              return '<span class="redact-target" data-index="' + (counter++) + '">' + match + '</span>'
            })
            
            element.innerHTML = new_html
            
            // Get all redaction target spans
            var target_spans = element.querySelectorAll('.redact-target')
            var overlay_ids = []
            
            var scroll_top = window.pageYOffset || document.documentElement.scrollTop
            var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
            
            // Get computed styles to check for any overhanging text
            var computed_style = window.getComputedStyle(element)
            
            // Add small buffer to ensure text is fully covered (especially for descenders, etc)
            var right_buffer = 10
            var left_buffer = 0
            var vertical_buffer = 2
            
            // Create overlay for each target
            for (var i = 0; i < target_spans.length; i++) {
              var bounding_rectangle = target_spans[i].getBoundingClientRect()
              var overlay = document.createElement('div')
            
              overlay.style.position = 'absolute'
              // Removing a buffer here since the box gets 'pushed' left when taking a screenshot
              overlay.style.left = (bounding_rectangle.left + scroll_left + left_buffer) + 'px'
              overlay.style.top = (bounding_rectangle.top + scroll_top - vertical_buffer) + 'px'
              overlay.style.width = (bounding_rectangle.width + (right_buffer * 2)) + 'px'
              overlay.style.height = (bounding_rectangle.height + (vertical_buffer * 2)) + 'px'
            
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
            """.formatted("blue", "white", "Email");

        driver.executeScript(script, element);
    }

    @Override
    public void redactIpAddress(final WebElement element) {
        final String script = """
            var element = arguments[0]
            
            var ipv4_regex = /((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)/g
            var ipv4_masked_regex = /((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){2}x\\.x/g
            var ipv6_regex = /([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}/g
            
            var original_html = element.innerHTML
            var counter = 0
            
            // Replace all matching patterns with spans
            // Order matters: check masked IPv4 before regular IPv4 to avoid partial matches
            var new_html = original_html
            new_html = new_html.replace(ipv4_masked_regex, function(match) {
              return '<span class="redact-target" data-index="' + (counter++) + '">' + match + '</span>'
            })
            new_html = new_html.replace(ipv6_regex, function(match) {
              return '<span class="redact-target" data-index="' + (counter++) + '">' + match + '</span>'
            })
            new_html = new_html.replace(ipv4_regex, function(match) {
              return '<span class="redact-target" data-index="' + (counter++) + '">' + match + '</span>'
            })
            
            element.innerHTML = new_html
            
            // Get all redaction target spans
            var target_spans = element.querySelectorAll('.redact-target')
            var overlay_ids = []
            
            var scroll_top = window.pageYOffset || document.documentElement.scrollTop
            var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
            
            // Get computed styles to check for any overhanging text
            var computed_style = window.getComputedStyle(element)
            
            // Add small buffer to ensure text is fully covered (especially for descenders, etc)
            var right_buffer = 10
            var left_buffer = 5
            var vertical_buffer = 2
            
            // Create overlay for each target
            for (var i = 0; i < target_spans.length; i++) {
              var bounding_rectangle = target_spans[i].getBoundingClientRect()
              var overlay = document.createElement('div')
            
              overlay.style.position = 'absolute'
              // Removing a buffer here since the box gets 'pushed' left when taking a screenshot
              overlay.style.left = (bounding_rectangle.left + scroll_left + left_buffer) + 'px'
              overlay.style.top = (bounding_rectangle.top + scroll_top - vertical_buffer) + 'px'
              overlay.style.width = (bounding_rectangle.width + (right_buffer * 2)) + 'px'
              overlay.style.height = (bounding_rectangle.height + (vertical_buffer * 2)) + 'px'
            
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
            """.formatted("yellow", "black", "IP");

        driver.executeScript(script, element);
    }

    private void redactElement(final WebElement element, final String overlayColour, final String description) {
        final String script = """
            var element = arguments[0]
            var bounding_rectangle = element.getBoundingClientRect()
            var scroll_top = window.pageYOffset || document.documentElement.scrollTop
            var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
            
            // Get computed styles to check for any overhanging text
            var computed_style = window.getComputedStyle(element)
            
            // Add small buffer to ensure text is fully covered (especially for descenders, etc)
            var right_buffer = 10
            var left_buffer = 5
            var vertical_buffer = 2
            
            var overlay = document.createElement('div')
            
            overlay.style.position = 'absolute'
            // Removing a buffer here since the box gets 'pushed' left when taking a screenshot
            overlay.style.left = (bounding_rectangle.left + scroll_left + left_buffer) + 'px'
            overlay.style.top = (bounding_rectangle.top + scroll_top - vertical_buffer) + 'px'
            overlay.style.width = (bounding_rectangle.width + (right_buffer * 2)) + 'px'
            overlay.style.height = (bounding_rectangle.height + (vertical_buffer * 2)) + 'px'
            
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
            """.formatted(overlayColour, "white", description);

        driver.executeScript(script, element);
    }
}
