const element = arguments[0]
const redaction_type = '%9$s'
const buffer_left = Number('%1$d')
const buffer_up = Number('%2$d')
const buffer_right = Number('%3$d')
const buffer_down = Number('%4$d')

if (redaction_type === 'blur') {
  element.style.filter = '%8$s'
} else {
  const bounding_rectangle = element.getBoundingClientRect()
  const scroll_top = window.pageYOffset || document.documentElement.scrollTop
  const scroll_left = window.pageXOffset || document.documentElement.scrollLeft
  const computed_style = window.getComputedStyle(element)

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
  overlay.id = 'redact-' + Date.now()
  document.body.appendChild(overlay)
}
