var email_regex = /[a-zA-Z0-9._+\-*]+@[a-zA-Z0-9.\-*]+\.[a-zA-Z*]{2,}/g
var ipv4_regex = /((25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)/g
var ipv4_masked_regex = /((25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\.){2}x\.x/g
var ipv6_full_regex = /([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}/g
var ipv6_partial_regex = /([0-9a-fA-F]{4}:){3,7}[0-9a-fA-F]{0,4}/g
var counter = 0

// Collects all text nodes under element into an array.
// Must be called before any DOM mutations to avoid TreeWalker/DOM mutation issues.
function collect_text_nodes(element) {
  var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false)
  var text_nodes = []
  var node
  while (node = walker.nextNode()) {
    text_nodes.push(node)
  }
  return text_nodes
}

// Wraps all regex matches in a text node with a <span> of the given class name.
function wrap_matches_in_text_node(text_node, regex, class_name) {
  var text = text_node.textContent
  regex.lastIndex = 0
  if (!regex.test(text)) return
  regex.lastIndex = 0
  var fragment = document.createDocumentFragment()
  var last_index = 0
  var match
  while ((match = regex.exec(text)) !== null) {
    if (match.index > last_index) {
      fragment.appendChild(document.createTextNode(text.slice(last_index, match.index)))
    }
    var span = document.createElement('span')
    span.className = class_name
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

// Applies wrap_matches_in_text_node to all text nodes under element for the given regex and class name.
// Order matters for IP regexes: check masked IPv4 before regular IPv4 to avoid partial matches.
function apply_regex(element, regex, class_name) {
  var text_nodes = collect_text_nodes(element)
  for (var t = 0; t < text_nodes.length; t++) {
    wrap_matches_in_text_node(text_nodes[t], regex, class_name)
  }
}

// Returns true if val matches any IP address regex variant.
function matches_any_ip_regex(val) {
  ipv4_masked_regex.lastIndex = 0
  ipv6_full_regex.lastIndex = 0
  ipv6_partial_regex.lastIndex = 0
  ipv4_regex.lastIndex = 0
  return ipv4_masked_regex.test(val) || ipv6_full_regex.test(val) || ipv6_partial_regex.test(val) || ipv4_regex.test(val)
}

// Wraps the sensitive portion of text_nodes (everything after prefix_len chars) in <span> elements
// and returns those spans.
function find_sensitive_spans(text_nodes, prefix_len) {
  var spans = []
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
      spans.push(span)
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
      spans.push(sensitive_span)
      past_prefix = true
    }
  }
  return spans
}
