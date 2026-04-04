var element = arguments[0]
var bounding_rectangle = element.getBoundingClientRect()
var scroll_top = window.pageYOffset || document.documentElement.scrollTop
var scroll_left = window.pageXOffset || document.documentElement.scrollLeft

// Get computed styles to check for any overhanging text
var computed_style = window.getComputedStyle(element)

var overlay = document.createElement('div')

overlay.style.position = 'absolute'
overlay.style.left = (bounding_rectangle.left + scroll_left - % d) + 'px'
overlay.style.top = (bounding_rectangle.top + scroll_top - % d) + 'px'
overlay.style.width = (bounding_rectangle.width + % d + % d) + 'px'
overlay.style.height = (bounding_rectangle.height + % d + % d) + 'px'

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
