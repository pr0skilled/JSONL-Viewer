package io.jsonlviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonPrettyTest {
    @Test fun formatsNestedObjectAndArray() {
        val out = JsonPretty.format("""{"id":1,"a":[1,2]}""")
        assertEquals("{\n  \"id\": 1,\n  \"a\": [\n    1,\n    2\n  ]\n}", out)
    }

    @Test fun bracesAndCommasInsideStringsAreLiteral() {
        val out = JsonPretty.format("""{"s":"}{,:"}""")
        assertEquals("{\n  \"s\": \"}{,:\"\n}", out)
    }

    @Test fun emptyContainersStayInline() {
        val out = JsonPretty.format("""{"a":{},"b":[]}""")
        assertEquals("{\n  \"a\": {},\n  \"b\": []\n}", out)
    }

    @Test fun previewCollapsesWhitespace() {
        assertEquals("{ \"id\": 1 }", JsonPretty.preview("""{  "id":   1 }"""))
    }

    @Test fun previewTruncatesLongInput() {
        val long = "{\"text\":\"" + "x".repeat(500) + "\"}"
        val p = JsonPretty.preview(long, maxChars = 20)
        assertEquals(21, p.length) // 20 chars + the ellipsis
        assertEquals('…', p.last())
    }
}
