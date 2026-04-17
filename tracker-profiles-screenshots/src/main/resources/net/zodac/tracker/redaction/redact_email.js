const element = arguments[0]
const redaction_type = '%9$s'
const buffer_left = Number('%1$d')
const buffer_up = Number('%2$d')
const buffer_right = Number('%3$d')
const buffer_down = Number('%4$d')

const scroll_top = window.pageYOffset || document.documentElement.scrollTop
const scroll_left = window.pageXOffset || document.documentElement.scrollLeft
const computed_style = window.getComputedStyle(element)
const overlay_ids = []

function apply_box(bounding_rectangle) {
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

apply_regex(element, email_regex, 'redact-email')

const target_spans = element.querySelectorAll('.redact-email')
for (let i = 0; i < target_spans.length; i++) {
  apply_redaction(target_spans[i])
}

// Also check 'value' attributes on the element and its descendants
const value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
for (let e = 0; e < value_elements.length; e++) {
  const val = value_elements[e].getAttribute('value')
  email_regex.lastIndex = 0
  if (val && email_regex.test(val)) {
    apply_redaction(value_elements[e])
  }
}

