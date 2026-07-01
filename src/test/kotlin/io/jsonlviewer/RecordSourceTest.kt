package io.jsonlviewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class RecordSourceTest {
    @Test fun readerReadsRecordsViaTempFileSource() {
        val tmp = Files.createTempFile("rec", ".jsonl")
        try {
            Files.write(tmp, "{\"id\":1}\n{\"id\":2}\n".toByteArray(StandardCharsets.UTF_8))
            val idx = JsonlLineIndex.build(Files.readAllBytes(tmp))
            val source = TempFileSource(tmp, Files.size(tmp))
            val reader = JsonlRecordReader(source)
            assertEquals("{\"id\":1}", reader.readRaw(idx, 0))
            assertEquals("{\"id\":2}", reader.readRaw(idx, 1))
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    @Test fun tempFileSourceCloseDeletesFile() {
        val tmp = Files.createTempFile("rec", ".jsonl")
        Files.write(tmp, "{\"id\":1}\n".toByteArray(StandardCharsets.UTF_8))
        val source = TempFileSource(tmp, Files.size(tmp))
        assertTrue(Files.exists(tmp))
        source.close()
        assertFalse(Files.exists(tmp))
    }

    @Test fun tempFileSourceOpenStreamReadsWholeContent() {
        val tmp = Files.createTempFile("rec", ".jsonl")
        try {
            Files.write(tmp, "a\nbb\n".toByteArray(StandardCharsets.UTF_8))
            val source = TempFileSource(tmp, Files.size(tmp))
            val bytes = source.openStream().use { it.readBytes() }
            assertEquals("a\nbb\n", String(bytes, StandardCharsets.UTF_8))
            assertEquals(5L, source.length)
        } finally {
            Files.deleteIfExists(tmp)
        }
    }
}
