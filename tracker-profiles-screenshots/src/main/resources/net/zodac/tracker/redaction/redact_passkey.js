const element = arguments[0]
const redaction_type = '%9$s'
const buffer_left = Number('%1$d')
const buffer_up = Number('%2$d')
const buffer_right = Number('%3$d')
const buffer_down = Number('%4$d')
const prefix_alternation = '%7$s'
const prefix_regex = new RegExp('^\\s*(' + prefix_alternation + ')\\s*:\\s*', 'i')
const prefix_match = prefix_regex.exec(element.textContent)

const scroll_top = window.pageYOffset || document.documentElement.scrollTop
const scroll_left = window.pageXOffset || document.documentElement.scrollLeft
const computed_style = window.getComputedStyle(element)
const overlay_ids = []

function apply_box(bounding_rectangle, show_label) {
  const overlay = document.createElement('div')
  overlay.style.position = 'absolute'
  overlay.style.left = (bounding_rectangle.left + scroll_left - buffer_left) + 'px'
  overlay.style.top = (bounding_rectangle.top + scroll_top - buffer_up) + 'px'
  overlay.style.width = (bounding_rectangle.width + buffer_left + buffer_right) + 'px'
  overlay.style.height = (bounding_rectangle.height + buffer_up + buffer_down) + 'px'
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
} else {
  const sensitive_spans = find_sensitive_spans(collect_text_nodes(element), prefix_match[0].length)
  for (let i = 0; i < sensitive_spans.length; i++) {
    apply_redaction(sensitive_spans[i], i === 0)
  }
}

// Also check 'value' attributes on the element and its descendants.
// Skipped when the whole element was blur-redacted, as value attributes within it are already covered.
if (prefix_match || redaction_type !== 'blur') {
  const value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
  for (let e = 0; e < value_elements.length; e++) {
    if (value_elements[e].getAttribute('value')) {
      apply_redaction(value_elements[e], true)
    }
  }
}

