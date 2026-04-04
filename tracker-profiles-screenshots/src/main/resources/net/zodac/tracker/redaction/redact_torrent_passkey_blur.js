var element = arguments[0]
var full_text = element.textContent
var prefix_regex = /^\s*(%1$s)\s*:\s*/i
var prefix_match = prefix_regex.exec(full_text)

if (!prefix_match) {
  element.style.filter = '%2$s'
  return
}

var prefix_len = prefix_match[0].length

// Collect all text nodes first to avoid TreeWalker/DOM mutation issues
var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false)
var text_nodes = []
var node
while (node = walker.nextNode()) {
  text_nodes.push(node)
}

var chars_seen = 0
var past_prefix = false
for (var i = 0; i < text_nodes.length; i++) {
  var tn = text_nodes[i]
  var tn_text = tn.textContent
  var tn_len = tn_text.length
  if (past_prefix) {
    var span = document.createElement('span')
    span.textContent = tn_text
    span.style.filter = '%2$s'
    tn.parentNode.replaceChild(span, tn)
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
    sensitive_span.style.filter = '%2$s'
    parent.replaceChild(sensitive_span, tn)
    past_prefix = true
  }
}

// Also check 'value' attributes on the element and its descendants
var value_elements = [element].concat(Array.from(element.querySelectorAll('[value]')))
for (var e = 0; e < value_elements.length; e++) {
  var val = value_elements[e].getAttribute('value')
  if (val) {
    value_elements[e].style.filter = '%2$s'
  }
}
