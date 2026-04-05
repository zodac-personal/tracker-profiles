var element = arguments[0]
var redaction_type = '%9$s'

var scroll_top = window.pageYOffset || document.documentElement.scrollTop
var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
var computed_style = window.getComputedStyle(element)
var overlay_ids = []

function apply_box(bounding_rectangle) {
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
  overlay.style.color = '%6$s'
  overlay.style.fontSize = computed_style.fontSize
  overlay.style.fontFamily = computed_style.fontFamily
  overlay.style.fontWeight = 'bold'
  overlay.textContent = '%7$s'
  overlay.id = 'redact-' + Date.now() + '-' + overlay_ids.length
  document.body.appendChild(overlay)
  overlay_ids.push(overlay.id)
}

function apply_redaction(target_element) {
  if (redaction_type === 'blur') {
    target_element.style.filter = '%8$s'
  } else {
    apply_box(target_element.getBoundingClientRect())
  }
}

apply_regex(element, ipv4_masked_regex, 'redact-ip')
apply_regex(element, ipv6_full_regex, 'redact-ip')
apply_regex(element, ipv6_partial_regex, 'redact-ip')
apply_regex(element, ipv4_regex, 'redact-ip')

var target_spans = element.querySelectorAll('.redact-ip')
for (var i = 0; i < target_spans.length; i++) {
  apply_redaction(target_spans[i])
}

// Also check 'value' attributes on the element and its descendants
var value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
for (var e = 0; e < value_elements.length; e++) {
  var val = value_elements[e].getAttribute('value')
  if (val && matches_any_ip_regex(val)) {
    apply_redaction(value_elements[e])
  }
}

if (redaction_type === 'box') {
  return overlay_ids.join(',')
}
