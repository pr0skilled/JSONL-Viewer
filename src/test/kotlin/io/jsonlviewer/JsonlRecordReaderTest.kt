package io.jsonlviewer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

class JsonlRecordReaderTest {
    @Test fun readsExactByteRangeForSecondRecord() {
        val tmp = File.createTempFile("rec", ".jsonl")
        try {
            tmp.writeBytes("{\"id\":1}\n{\"id\":2}\n".toByteArray())
            val idx = JsonlLineIndex.build(tmp.readBytes())
            val bytes = readFromPath(tmp.toPath(), idx.lineStart(1), idx.lineLength(1).toInt())
            assertEquals("{\"id\":2}\n", String(bytes, StandardCharsets.UTF_8))
        } finally {
            tmp.delete()
        }
    }

    @Test fun readsUtf8MultiByteContent() {
        val tmp = File.createTempFile("rec", ".jsonl")
        try {
            tmp.writeBytes("{\"t\":\"Gärtner\"}\n".toByteArray(StandardCharsets.UTF_8))
            val idx = JsonlLineIndex.build(tmp.readBytes())
            val bytes = readFromPath(tmp.toPath(), idx.lineStart(0), idx.lineLength(0).toInt())
            assertEquals("{\"t\":\"Gärtner\"}\n", String(bytes, StandardCharsets.UTF_8))
        } finally {
            tmp.delete()
        }
    }
}
