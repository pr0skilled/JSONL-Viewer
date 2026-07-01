package io.jsonlviewer

import com.intellij.json.jsonLines.JsonLinesFileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Guards that .jsonl and .ndjson extensions are handled by the platform's
 * built-in JSON Lines file type (name "JSON-lines"). Our plugin does not
 * register a competing file type — it adds a fileEditorProvider for these
 * extensions and otherwise relies on the built-in JSON Lines language.
 */
class JsonlFileTypeTest : BasePlatformTestCase() {
    fun testJsonlExtensionMapsToBuiltInJsonLinesFileType() {
        val ft = FileTypeManager.getInstance().getFileTypeByFileName("data.jsonl")
        assertEquals(JsonLinesFileType.INSTANCE, ft)
        assertEquals("JSON-lines", ft.name)
    }

    fun testNdjsonExtensionMapsToBuiltInJsonLinesFileType() {
        val ft = FileTypeManager.getInstance().getFileTypeByFileName("logs.ndjson")
        assertEquals(JsonLinesFileType.INSTANCE, ft)
        assertEquals("JSON-lines", ft.name)
    }

    fun testGzippedJsonlMapsToJsonlGzFileType() {
        val ft = FileTypeManager.getInstance().getFileTypeByFileName("data.jsonl.gz")
        assertEquals(JsonlGzFileType, ft)
        assertEquals("Gzipped JSON Lines", ft.name)
        assertTrue(ft.isBinary)
    }

    fun testGzippedNdjsonMapsToJsonlGzFileType() {
        val ft = FileTypeManager.getInstance().getFileTypeByFileName("logs.ndjson.gz")
        assertEquals(JsonlGzFileType, ft)
    }
}
