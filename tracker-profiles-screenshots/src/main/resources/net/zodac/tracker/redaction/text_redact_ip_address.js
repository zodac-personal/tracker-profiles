if (!window.__textRedactIpAddress) {
    window.__textRedactIpAddress = function(element, redactionText) {
        const NON_BREAKING_SPACE = ' '
        const ipv4_masked_regex = /((25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\.){2}x\.x/g
        const ipv6_full_regex = /([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}/g
        const ipv6_partial_regex = /([0-9a-fA-F]{4}:){3,7}[0-9a-fA-F]{0,4}/g
        const ipv4_regex = /((25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)/g

        function replacement(length) {
            if (redactionText.length >= length) {
                return redactionText.substring(0, length)
            }
            return redactionText + NON_BREAKING_SPACE.repeat(length - redactionText.length)
        }

        function replaceInText(text) {
            let result = text.replace(ipv4_masked_regex, match => replacement(match.length))
            result = result.replace(ipv6_full_regex, match => replacement(match.length))
            result = result.replace(ipv6_partial_regex, match => replacement(match.length))
            result = result.replace(ipv4_regex, match => replacement(match.length))
            return result
        }

        const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null)
        const text_nodes = []
        let node
        while ((node = walker.nextNode())) {
            text_nodes.push(node)
        }

        for (let i = 0; i < text_nodes.length; i++) {
            const tn = text_nodes[i]
            const replaced = replaceInText(tn.textContent)
            if (replaced !== tn.textContent) {
                tn.textContent = replaced
            }
        }
    }
}
