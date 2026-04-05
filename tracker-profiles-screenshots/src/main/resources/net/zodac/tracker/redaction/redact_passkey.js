var element = arguments[0]
var redaction_type = '%9$s'
var prefix_regex = /^\s*(%7$s)\s*:\s*/i
var prefix_match = prefix_regex.exec(element.textContent)

var scroll_top = window.pageYOffset || document.documentElement.scrollTop
var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
var computed_style = window.getComputedStyle(element)
var overlay_ids = []

function apply_box(bounding_rectangle, show_label) {
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
  if (show_label) {
    overlay.textContent = '%6$s'
  }
  overlay.id = 'redact-' + Date.now() + '-' + overlay_ids.length
  document.body.appendChild(overlay)
  overlay_ids.push(overlay.id)
}

function apply_redaction(target_element, show_label) {
  if (redaction_type === 'blur') {
    target_element.style.filter = '%8$s'
  } else {
    apply_box(target_element.getBoundingClientRect(), show_label)
  }
}

if (!prefix_match) {
  apply_redaction(element, true)
  if (redaction_type === 'blur') {
    return  // whole element is blurred; value attributes within it are already covered
  }
} else {
  var sensitive_spans = find_sensitive_spans(collect_text_nodes(element), prefix_match[0].length)
  for (var i = 0; i < sensitive_spans.length; i++) {
    apply_redaction(sensitive_spans[i], i === 0)
  }
}

// Also check 'value' attributes on the element and its descendants
var value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
for (var e = 0; e < value_elements.length; e++) {
  if (value_elements[e].getAttribute('value')) {
    apply_redaction(value_elements[e], true)
  }
}

if (redaction_type === 'box') {
  return overlay_ids.join(',')
}
