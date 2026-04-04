var element = arguments[0]
var full_text = element.textContent
var prefix_regex = /^\s*(%7$s)\s*:\s*/i
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

// Also check 'value' attributes on the element and its descendants
var value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
for (var e = 0; e < value_elements.length; e++) {
  var val = value_elements[e].getAttribute('value')
  if (val) {
    var bounding_rectangle = value_elements[e].getBoundingClientRect()
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
    overlay.textContent = '%6$s'
    overlay.id = 'redact-' + Date.now() + '-v-' + e
    document.body.appendChild(overlay)
    overlay_ids.push(overlay.id)
  }
}
return overlay_ids.join(',')
