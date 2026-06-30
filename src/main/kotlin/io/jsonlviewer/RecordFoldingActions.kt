package io.jsonlviewer

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware

/** Action group id of the record detail viewer's right-click menu (see plugin.xml). */
internal const val RECORD_VIEWER_POPUP_GROUP = "io.jsonlviewer.RecordViewerPopup"

/** Expands or collapses every fold region in [editor] in a single batch operation. */
internal fun setRecordFoldsExpanded(editor: Editor, expanded: Boolean) {
    editor.foldingModel.runBatchFoldingOperation {
        editor.foldingModel.allFoldRegions.forEach { it.isExpanded = expanded }
    }
}

private fun editorWithFolds(e: AnActionEvent): Editor? =
    e.getData(CommonDataKeys.EDITOR)?.takeIf { it.foldingModel.allFoldRegions.isNotEmpty() }

/** Right-click action on the record detail viewer: collapse every nested object/array. */
class CollapseAllRecordFoldsAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        setRecordFoldsExpanded(e.getData(CommonDataKeys.EDITOR) ?: return, false)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = editorWithFolds(e) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

/** Right-click action on the record detail viewer: expand every nested object/array. */
class ExpandAllRecordFoldsAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        setRecordFoldsExpanded(e.getData(CommonDataKeys.EDITOR) ?: return, true)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = editorWithFolds(e) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
