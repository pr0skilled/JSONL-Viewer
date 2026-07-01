package io.jsonlviewer

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.GZIPOutputStream

class GzipSourceTest {
    private fun gzip(bytes: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(bytes) }
        return bos.toByteArray()
    }

    @Test fun decompressedTempMatchesUncompressedIndexAndRecords() {
        val original = "{\"id\":1}\n{\"t\":\"Gärtner\"}\n{\"id\":3}".toByteArray(StandardCharsets.UTF_8)
        val temp = decompressToTempFile(gzip(original).inputStream(), null, 0)
        try {
            assertArrayEquals(original, Files.readAllBytes(temp))
            val source = TempFileSource(temp, Files.size(temp))
            val idx = source.openStream().use { JsonlLineIndex.build(it, source.length) }
            assertEquals(3, idx.count)
            val reader = JsonlRecordReader(source)
            assertEquals("{\"id\":1}", reader.readRaw(idx, 0))
            assertEquals("{\"t\":\"Gärtner\"}", reader.readRaw(idx, 1))
            assertEquals("{\"id\":3}", reader.readRaw(idx, 2)) // final line, no trailing newline
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    @Test fun searchWorksOverDecompressedTemp() {
        val original = "{\"label\":\"POSITIVE\"}\n{\"label\":\"negative\"}\n".toByteArray(StandardCharsets.UTF_8)
        val temp = decompressToTempFile(gzip(original).inputStream(), null, 0)
        try {
            val source = TempFileSource(temp, Files.size(temp))
            val idx = source.openStream().use { JsonlLineIndex.build(it, source.length) }
            val matches = source.openStream().use { JsonlSearch.find(it, idx, "positive", source.length) }
            assertArrayEquals(intArrayOf(0), matches)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    @Test fun rejectsNonGzipInput() {
        try {
            decompressToTempFile("{\"id\":1}\n".toByteArray().inputStream(), null, 0)
            fail("expected IOException for non-gzip input")
        } catch (e: IOException) {
            // expected — GZIPInputStream rejects a bad header
        }
    }
}
