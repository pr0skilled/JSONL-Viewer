package io.jsonlviewer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonlFormatTest {
    @Test fun acceptsPlainJsonlVariants() {
        assertTrue(JsonlFormat.acceptsName("data.jsonl"))
        assertTrue(JsonlFormat.acceptsName("logs.ndjson"))
        assertTrue(JsonlFormat.acceptsName("x.jsonlines"))
        assertTrue(JsonlFormat.acceptsName("y.ldjson"))
        assertTrue(JsonlFormat.acceptsName("DATA.JSONL")) // case-insensitive
    }

    @Test fun acceptsGzippedJsonlVariants() {
        assertTrue(JsonlFormat.acceptsName("data.jsonl.gz"))
        assertTrue(JsonlFormat.acceptsName("logs.ndjson.gz"))
        assertTrue(JsonlFormat.acceptsName("x.jsonlines.gz"))
        assertTrue(JsonlFormat.acceptsName("y.ldjson.gz"))
        assertTrue(JsonlFormat.acceptsName("DATA.JSONL.GZ"))
    }

    @Test fun rejectsUnrelatedNames() {
        assertFalse(JsonlFormat.acceptsName("archive.gz"))
        assertFalse(JsonlFormat.acceptsName("notes.txt.gz"))
        assertFalse(JsonlFormat.acceptsName("data.json"))
        assertFalse(JsonlFormat.acceptsName("data.zip"))
        assertFalse(JsonlFormat.acceptsName("README"))
    }

    @Test fun isGzipDetection() {
        assertTrue(JsonlFormat.isGzipName("data.jsonl.gz"))
        assertFalse(JsonlFormat.isGzipName("data.jsonl"))
        assertFalse(JsonlFormat.isGzipName("archive.gz"))
    }
}
