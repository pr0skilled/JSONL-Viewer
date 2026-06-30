package io.jsonlviewer

import com.intellij.json.JsonFileType
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Guards the detail-pane folding. A standalone viewer editor is not driven by the
 * folding daemon, so fold regions must be built explicitly — without that the nested
 * objects/arrays of a record show no fold icons.
 */
class JsonlDetailFoldingTest : BasePlatformTestCase() {

    private val prettyRecord = JsonPretty.format(
        """{"id":1,"profession_tags":["a","b","c"],"meta":{"x":1,"y":2}}"""
    )

    fun testPreviewEditorBuildsNestedFoldRegions() {
        val editor = createRecordPreviewEditor(project, prettyRecord)
        try {
            buildPreviewFolding(editor)
            val placeholders = editor.foldingModel.allFoldRegions.map { it.placeholderText }
            assertTrue(
                "expected an array fold region, got $placeholders",
                placeholders.contains("[...]")
            )
            assertTrue(
                "expected an object fold region, got $placeholders",
                placeholders.contains("{...}")
            )
        } finally {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    fun testCollapseAndExpandAllToggleEveryRegion() {
        val editor = createRecordPreviewEditor(project, prettyRecord)
        try {
            buildPreviewFolding(editor)
            setRecordFoldsExpanded(editor, false)
            assertTrue(
                "collapse all should leave every region collapsed",
                editor.foldingModel.allFoldRegions.all { !it.isExpanded }
            )
            setRecordFoldsExpanded(editor, true)
            assertTrue(
                "expand all should leave every region expanded",
                editor.foldingModel.allFoldRegions.all { it.isExpanded }
            )
        } finally {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    fun testBareEditorHasNoFoldRegionsWithoutAnExplicitPass() {
        // Documents the root cause: an EditorFactory viewer editor never gets fold
        // regions on its own; createRecordPreviewEditor is what builds them.
        val light = LightVirtualFile("record.json", JsonFileType.INSTANCE, prettyRecord)
        val document = FileDocumentManager.getInstance().getDocument(light)!!
        val editor = EditorFactory.getInstance()
            .createEditor(document, project, JsonFileType.INSTANCE, true) as EditorEx
        try {
            assertTrue(editor.foldingModel.allFoldRegions.isEmpty())
        } finally {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }
}
