package io.jsonlviewer

/**
 * String-aware JSON formatting for a single record. Both functions walk the raw
 * text honoring JSON string/escape state, so braces, commas and colons inside
 * string values never affect structure. Tolerant of malformed input (best effort).
 */
object JsonPretty {
    private const val INDENT = "  "

    /** Whitespace-collapsed, single-line, truncated preview of a raw record. */
    fun preview(raw: String, maxChars: Int = 200): String {
        val sb = StringBuilder(minOf(raw.length, maxChars + 1))
        var inString = false
        var escaped = false
        var prevSpace = false
        for (c in raw) {
            if (inString) {
                sb.append(c)
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                prevSpace = false
            } else when (c) {
                '"' -> { inString = true; sb.append(c); prevSpace = false }
                ' ', '\t', '\n', '\r' -> if (!prevSpace) { sb.append(' '); prevSpace = true }
                else -> { sb.append(c); prevSpace = false }
            }
            if (sb.length > maxChars) {
                sb.setLength(maxChars); sb.append('…'); break
            }
        }
        return sb.toString().trim()
    }

    /** Pretty-print a single JSON record into indented multi-line text. */
    fun format(raw: String): String {
        val sb = StringBuilder(raw.length + raw.length / 2)
        var inString = false
        var escaped = false
        var depth = 0
        var i = 0
        val n = raw.length
        fun newline(d: Int) { sb.append('\n'); repeat(d) { sb.append(INDENT) } }
        while (i < n) {
            val c = raw[i]
            if (inString) {
                sb.append(c)
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                i++; continue
            }
            when (c) {
                '"' -> { inString = true; sb.append(c) }
                '{', '[' -> {
                    val close = if (c == '{') '}' else ']'
                    val j = nextNonWs(raw, i + 1)
                    if (j < n && raw[j] == close) { sb.append(c).append(close); i = j }
                    else { depth++; sb.append(c); newline(depth) }
                }
                '}', ']' -> { depth = maxOf(0, depth - 1); newline(depth); sb.append(c) }
                ',' -> { sb.append(c); newline(depth) }
                ':' -> { sb.append(": "); i = nextNonWs(raw, i + 1) - 1 }
                ' ', '\t', '\n', '\r' -> { /* drop structural whitespace */ }
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    private fun nextNonWs(s: String, from: Int): Int {
        var k = from
        while (k < s.length && (s[k] == ' ' || s[k] == '\t' || s[k] == '\n' || s[k] == '\r')) k++
        return k
    }
}
