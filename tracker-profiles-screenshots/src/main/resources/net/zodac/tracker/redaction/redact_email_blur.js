var element = arguments[0]
var blur_definition = '%s'
var email_regex = /[a-zA-Z0-9._+\-*]+@[a-zA-Z0-9.\-*]+\.[a-zA-Z*]{2,}/g
var counter = 0

// Use TreeWalker to process only text nodes, so attribute values are never modified
var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false)
var text_nodes = []
var node
while (node = walker.nextNode()) {
  text_nodes.push(node)
}

for (var t = 0; t < text_nodes.length; t++) {
  var text_node = text_nodes[t]
  var text = text_node.textContent
  email_regex.lastIndex = 0
  if (!email_regex.test(text)) continue
  email_regex.lastIndex = 0
  var fragment = document.createDocumentFragment()
  var last_index = 0
  var match
  while ((match = email_regex.exec(text)) !== null) {
    if (match.index > last_index) {
      fragment.appendChild(document.createTextNode(text.slice(last_index, match.index)))
    }
    var span = document.createElement('span')
    span.className = 'redact-blur-email'
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

// Apply blur directly to each matched span
var target_spans = element.querySelectorAll('.redact-blur-email')
for (var i = 0; i < target_spans.length; i++) {
  target_spans[i].style.filter = blur_definition
}

// Also check 'value' attributes on the element and its descendants
var value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
for (var e = 0; e < value_elements.length; e++) {
  var val = value_elements[e].getAttribute('value')
  email_regex.lastIndex = 0
  if (val && email_regex.test(val)) {
    value_elements[e].style.filter = blur_definition
  }
}
