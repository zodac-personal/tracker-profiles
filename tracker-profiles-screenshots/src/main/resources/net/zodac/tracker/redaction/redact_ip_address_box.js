var element = arguments[0]

var ipv4_regex = /((25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)/g
var ipv4_masked_regex = /((25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\.){2}x\.x/g
var ipv6_full_regex = /([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}/g
var ipv6_partial_regex = /([0-9a-fA-F]{4}:){3,7}[0-9a-fA-F]{0,4}/g
var counter = 0

var scroll_top = window.pageYOffset || document.documentElement.scrollTop
var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
var computed_style = window.getComputedStyle(element)

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
    span.className = 'redact-box-ip'
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

// Get all redaction target spans
var target_spans = element.querySelectorAll('.redact-box-ip')
var overlay_ids = []

// Create overlay for each target
for (var i = 0; i < target_spans.length; i++) {
  var bounding_rectangle = target_spans[i].getBoundingClientRect()
  var overlay = document.createElement('div')

  overlay.style.position = 'absolute'
  overlay.style.left = (bounding_rectangle.left + scroll_left - %1$d) + 'px'
  overlay.style.top = (bounding_rectangle.top + scroll_top - %2$d) + 'px'
  overlay.style.width = (bounding_rectangle.width + %1$d + %4$d) + 'px'
  overlay.style.height = (bounding_rectangle.height + %2$d + %6$d) + 'px'

  overlay.style.backgroundColor = '%7$s'
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
  overlay.style.color = '%8$s'
  overlay.style.fontSize = computed_style.fontSize
  overlay.style.fontFamily = computed_style.fontFamily
  overlay.style.fontWeight = 'bold'
  overlay.textContent = '%9$s'
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
    var bounding_rectangle = value_elements[e].getBoundingClientRect()
    var overlay = document.createElement('div')
    overlay.style.position = 'absolute'
    overlay.style.left = (bounding_rectangle.left + scroll_left - %1$d) + 'px'
    overlay.style.top = (bounding_rectangle.top + scroll_top - %2$d) + 'px'
    overlay.style.width = (bounding_rectangle.width + %1$d + %4$d) + 'px'
    overlay.style.height = (bounding_rectangle.height + %2$d + %6$d) + 'px'
    overlay.style.backgroundColor = '%7$s'
    overlay.style.zIndex = '9999'
    overlay.style.pointerEvents = 'none'
    overlay.style.boxSizing = 'border-box'
    overlay.style.display = 'flex'
    overlay.style.alignItems = 'center'
    overlay.style.justifyContent = 'center'
    overlay.style.color = '%8$s'
    overlay.style.fontSize = computed_style.fontSize
    overlay.style.fontFamily = computed_style.fontFamily
    overlay.style.fontWeight = 'bold'
    overlay.textContent = '%9$s'
    overlay.id = 'redact-' + Date.now() + '-v-' + e
    document.body.appendChild(overlay)
    overlay_ids.push(overlay.id)
  }
}

return overlay_ids.join(',')
