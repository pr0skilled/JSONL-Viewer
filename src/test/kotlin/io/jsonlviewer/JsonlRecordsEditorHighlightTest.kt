package io.jsonlviewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonlRecordsEditorHighlightTest {
    @Test fun findsAsciiMatchCaseInsensitively() {
        val text = "{\"label\": \"POSITIVE\"}"
        val range = searchMatchRange(text, "positive")!!
        assertEquals("POSITIVE", text.substring(range.first, range.last + 1))
    }

    @Test fun nullWhenNoMatch() {
        assertNull(searchMatchRange("{\"a\":1}", "zzz"))
    }

    @Test fun nullWhenQueryNullOrEmpty() {
        assertNull(searchMatchRange("abc", null))
        assertNull(searchMatchRange("abc", ""))
    }

    @Test fun unicodeLengthChangingCharBeforeMatchDoesNotThrow() {
        // 'İ' (U+0130) lowercases to two code units in the default locale; the old
        // lowercase()-offset approach could throw StringIndexOutOfBounds here.
        val text = "İstanbul role POSITIVE"
        val range = searchMatchRange(text, "positive")!!
        assertEquals("POSITIVE", text.substring(range.first, range.last + 1))
    }
}
