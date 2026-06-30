package io.jsonlviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonlLineIndexTest {
    @Test fun threeLinesWithTrailingNewline() {
        val idx = JsonlLineIndex.build("a\nbb\nccc\n".toByteArray())
        assertEquals(3, idx.count)
        assertEquals(0L, idx.lineStart(0)); assertEquals(2L, idx.lineEnd(0))
        assertEquals(2L, idx.lineStart(1)); assertEquals(5L, idx.lineEnd(1))
        assertEquals(5L, idx.lineStart(2)); assertEquals(9L, idx.lineEnd(2))
    }

    @Test fun lastLineWithoutTrailingNewline() {
        val idx = JsonlLineIndex.build("a\nbb".toByteArray())
        assertEquals(2, idx.count)
        assertEquals(2L, idx.lineStart(1)); assertEquals(4L, idx.lineEnd(1))
    }

    @Test fun emptyFileHasNoRecords() {
        assertEquals(0, JsonlLineIndex.build(ByteArray(0)).count)
    }

    @Test fun newlinesInsideStringsAreNotRecordBreaks() {
        // The real data escapes inner newlines as \n (0x5C 0x6E), so only a raw
        // 0x0A separates records. This input has one record with an escaped \n.
        val idx = JsonlLineIndex.build("{\"r\":\"a\\nb\"}\n".toByteArray())
        assertEquals(1, idx.count)
    }
}
