var element = arguments[0]
var redaction_type = '%9$s'

if (redaction_type === 'blur') {
  element.style.filter = '%8$s'
} else {
  var bounding_rectangle = element.getBoundingClientRect()
  var scroll_top = window.pageYOffset || document.documentElement.scrollTop
  var scroll_left = window.pageXOffset || document.documentElement.scrollLeft
  var computed_style = window.getComputedStyle(element)

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
  overlay.id = 'redact-' + Date.now()
  document.body.appendChild(overlay)
  return overlay.id
}
