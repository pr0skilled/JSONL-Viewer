package io.jsonlviewer

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class JsonlSearchTest {
    @Test fun findsCaseInsensitiveSubstringMatches() {
        val data = "{\"label\":\"POSITIVE\"}\n{\"label\":\"negative\"}\n{\"x\":\"positive vibes\"}\n"
        val idx = JsonlLineIndex.build(data.toByteArray())
        val m = JsonlSearch.find(data.byteInputStream(), idx, "positive", data.length.toLong())
        assertArrayEquals(intArrayOf(0, 2), m)
    }

    @Test fun matchesLastLineWithoutTrailingNewline() {
        val data = "{\"a\":1}\n{\"a\":2}"
        val idx = JsonlLineIndex.build(data.toByteArray())
        val m = JsonlSearch.find(data.byteInputStream(), idx, "\"a\":2", data.length.toLong())
        assertArrayEquals(intArrayOf(1), m)
    }

    @Test fun emptyQueryReturnsNothing() {
        val data = "{\"a\":1}\n"
        val idx = JsonlLineIndex.build(data.toByteArray())
        assertArrayEquals(IntArray(0), JsonlSearch.find(data.byteInputStream(), idx, "", data.length.toLong()))
    }
}
