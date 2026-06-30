package io.jsonlviewer

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsonlRecordsEditorProviderTest : BasePlatformTestCase() {
    private val provider = JsonlRecordsEditorProvider()

    fun testAcceptsJsonlAndNdjsonByExtension() {
        val jsonl = myFixture.configureByText("a.jsonl", "{\"id\":1}\n").virtualFile
        val ndjson = myFixture.configureByText("b.ndjson", "{\"id\":2}\n").virtualFile
        assertTrue(provider.accept(project, jsonl))
        assertTrue(provider.accept(project, ndjson))
    }

    fun testRejectsOtherExtensions() {
        val txt = myFixture.configureByText("c.txt", "hello\n").virtualFile
        assertFalse(provider.accept(project, txt))
    }

    fun testEditorTypeIdAndPolicy() {
        assertEquals("jsonl-records-viewer", provider.editorTypeId)
        assertEquals(
            com.intellij.openapi.fileEditor.FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR,
            provider.policy
        )
    }
}
