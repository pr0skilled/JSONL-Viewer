package io.jsonlviewer

import com.intellij.json.JsonFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.AbstractListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

/**
 * Case-insensitive index range of the first [query] match within [text], or null
 * if [query] is null/blank or not found. Matches on the original string so the
 * returned offsets are always valid for [text] — unlike searching a lowercased
 * copy, whose length can differ from the original for some Unicode characters.
 */
internal fun searchMatchRange(text: String, query: String?): IntRange? {
    if (query.isNullOrEmpty()) return null
    val match = Regex(Regex.escape(query), RegexOption.IGNORE_CASE).find(text) ?: return null
    return match.range
}

/**
 * Builds the read-only JSON viewer editor for a single pretty-printed record.
 * The caller owns the returned editor and must release it via [EditorFactory.releaseEditor].
 *
 * Fold regions are NOT built here — see [buildPreviewFolding]. A standalone editor
 * created via [EditorFactory] is not driven by the code-folding daemon, so folds must
 * be built explicitly, but that pass must run AFTER the editor is shown (see the note
 * on [buildPreviewFolding]).
 */
internal fun createRecordPreviewEditor(project: Project, prettyText: String): EditorEx {
    val light = LightVirtualFile("record.json", JsonFileType.INSTANCE, prettyText)
    val document = FileDocumentManager.getInstance().getDocument(light)
        ?: EditorFactory.getInstance().createDocument(prettyText)
    val editor = EditorFactory.getInstance()
        .createEditor(document, project, JsonFileType.INSTANCE, true) as EditorEx
    editor.settings.apply {
        isLineNumbersShown = false
        isFoldingOutlineShown = true
        isLineMarkerAreaShown = false
        isUseSoftWraps = true
        additionalLinesCount = 0
        additionalColumnsCount = 0
    }
    // Right-click menu: Expand All / Collapse All (see plugin.xml group).
    editor.contextMenuGroupId = RECORD_VIEWER_POPUP_GROUP
    return editor
}

/**
 * Adds fold regions for every multi-line `{...}`/`[...]` pair in the editor's
 * pretty-printed JSON, directly on the editor's [com.intellij.openapi.editor.FoldingModel].
 *
 * This deliberately does NOT use the platform's folding daemon / CodeFoldingManager:
 * that pipeline is unreliable for a standalone (non-project) editor and behaves
 * differently across IDEs/products (e.g. Rider's split frontend/backend). Since we
 * pretty-printed the text ourselves, we already know every bracket span — scanning it
 * with the same string-aware logic and adding the regions ourselves works identically
 * in every IntelliJ-based IDE, with no PSI, daemon, or write-safe-context dependency.
 *
 * Regions are added expanded; the user collapses them. Only multi-line pairs get a
 * region (single-line `{}`/`[]` never show a fold gutter icon anyway).
 */
internal fun buildPreviewFolding(editor: Editor) {
    val document = editor.document
    val text = document.charsSequence
    val n = text.length
    // (startOffset, endOffset, placeholder) for each foldable bracket pair.
    val spans = ArrayList<Triple<Int, Int, String>>()
    val stack = ArrayDeque<Int>()
    var inString = false
    var escaped = false
    var i = 0
    while (i < n) {
        val c = text[i]
        when {
            inString -> when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> inString = false
            }
            c == '"' -> inString = true
            c == '{' || c == '[' -> stack.addLast(i)
            c == '}' || c == ']' -> {
                val start = stack.removeLastOrNull()
                if (start != null && document.getLineNumber(start) != document.getLineNumber(i)) {
                    val placeholder = if (text[start] == '{') "{...}" else "[...]"
                    spans.add(Triple(start, i + 1, placeholder))
                }
            }
        }
        i++
    }
    editor.foldingModel.runBatchFoldingOperation {
        for ((start, end, placeholder) in spans) {
            editor.foldingModel.addFoldRegion(start, end, placeholder)?.isExpanded = true
        }
    }
}

/**
 * Read-only viewer that renders a .jsonl/.ndjson file as a virtualized list of
 * one-line record previews (top) plus a pretty-printed, foldable JSON view of the
 * selected record (bottom). The file is streamed — never loaded into a Document or
 * PSI — so size limits and the single-visual-line folding limitation never apply.
 */
class JsonlRecordsEditor(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor {

    private val pcs = PropertyChangeSupport(this)
    @Volatile private var source: RecordSource? = null
    @Volatile private var reader: JsonlRecordReader? = null

    @Volatile private var index: JsonlLineIndex? = null
    @Volatile private var disposed = false
    private var baseCount = 0
    private var currentQuery: String? = null

    private val previewCache = object : LinkedHashMap<Int, String>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, String>): Boolean = size > 4096
    }

    private val searchField = SearchTextField()
    private val statusLabel = JBLabel("Indexing…")
    private val recordList = JBList<Int>()
    private val detailHost = JPanel(BorderLayout())
    private var detailEditor: Editor? = null
    private val root: JComponent

    init {
        recordList.fixedCellHeight = JBUI.scale(22)
        recordList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        recordList.cellRenderer = RecordRenderer()
        recordList.model = ArrayModel(IntArray(0))
        recordList.addListSelectionListener { e -> if (!e.valueIsAdjusting) showSelected() }

        searchField.textEditor.emptyText.text = "Search text, then Enter"
        searchField.textEditor.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> runSearch()
                    KeyEvent.VK_ESCAPE -> { searchField.text = ""; clearFilter() }
                }
            }
        })

        val top = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, 4)
            add(searchField, BorderLayout.CENTER)
            add(statusLabel, BorderLayout.EAST)
        }
        val splitter = JBSplitter(true, 0.5f).apply {
            firstComponent = JBScrollPane(recordList)
            secondComponent = detailHost
        }
        root = JPanel(BorderLayout()).apply {
            add(top, BorderLayout.NORTH)
            add(splitter, BorderLayout.CENTER)
        }
        startIndexing()
    }

    private fun startIndexing() {
        object : Task.Backgroundable(project, "Indexing ${file.name}", true) {
            override fun run(indicator: ProgressIndicator) {
                val src: RecordSource = try {
                    buildSource(indicator)
                } catch (e: Exception) {
                    showIndexError(e)
                    return
                }
                if (disposed) { src.close(); return }
                val idx = try {
                    src.openStream().use { JsonlLineIndex.build(it, src.length, indicator) }
                } catch (e: Exception) {
                    src.close()
                    showIndexError(e)
                    return
                }
                ApplicationManager.getApplication().invokeLater({
                    if (disposed) { src.close(); return@invokeLater }
                    source = src
                    reader = JsonlRecordReader(src)
                    index = idx
                    baseCount = idx.count
                    currentQuery = null
                    recordList.model = RangeModel(idx.count)
                    statusLabel.text = "${idx.count} records"
                    if (idx.count > 0) recordList.selectedIndex = 0
                }, ModalityState.any())
            }
        }.queue()
    }

    /**
     * Builds the byte source for [file]. Plain files wrap the VirtualFile
     * directly; gzip files are streamed once into a temp file (see
     * [decompressToTempFile]) that the returned [TempFileSource] owns.
     */
    private fun buildSource(indicator: ProgressIndicator): RecordSource {
        if (!JsonlFormat.isGzip(file)) return VirtualFileSource(file)
        indicator.text = "Decompressing ${file.name}"
        val temp = file.inputStream.use { decompressToTempFile(it, indicator, file.length) }
        return TempFileSource(temp, java.nio.file.Files.size(temp))
    }

    private fun showIndexError(e: Exception) {
        ApplicationManager.getApplication().invokeLater(
            { if (!disposed) statusLabel.text = "Failed to index: ${e.message}" },
            ModalityState.any()
        )
    }

    private fun runSearch() {
        val idx = index ?: return
        val query = searchField.text.trim()
        if (query.isEmpty()) { clearFilter(); return }
        object : Task.Backgroundable(project, "Searching ${file.name}", true) {
            override fun run(indicator: ProgressIndicator) {
                val src = source ?: return
                val matches = try {
                    src.openStream().use {
                        JsonlSearch.find(it, idx, query, src.length, indicator)
                    }
                } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
                    throw e
                } catch (e: Exception) {
                    if (!disposed) showIndexError(e)
                    return
                }
                ApplicationManager.getApplication().invokeLater({
                    if (disposed) return@invokeLater
                    currentQuery = query
                    recordList.model = ArrayModel(matches)
                    statusLabel.text = "${matches.size} matches"
                    if (matches.isNotEmpty()) recordList.selectedIndex = 0
                    recordList.repaint()
                }, ModalityState.any())
            }
        }.queue()
    }

    private fun clearFilter() {
        currentQuery = null
        recordList.model = RangeModel(baseCount)
        statusLabel.text = "$baseCount records"
        recordList.repaint()
    }

    private fun previewFor(recordIndex: Int): String {
        previewCache[recordIndex]?.let { return it }
        val idx = index ?: return ""
        val r = reader ?: return ""
        val preview = JsonPretty.preview(r.readRaw(idx, recordIndex))
        previewCache[recordIndex] = preview
        return preview
    }

    private fun showSelected() {
        if (disposed) return
        val idx = index ?: return
        val r = reader ?: return
        val recordIndex = recordList.selectedValue ?: return
        setDetailText(JsonPretty.format(r.readRaw(idx, recordIndex)))
    }

    private fun setDetailText(text: String) {
        if (disposed) return
        releaseDetailEditor()
        val editor = createRecordPreviewEditor(project, text)
        detailEditor = editor
        detailHost.removeAll()
        detailHost.add(editor.component, BorderLayout.CENTER)
        detailHost.revalidate()
        detailHost.repaint()
        // Build folding only after the editor is shown — doing it synchronously before
        // the first paint corrupted the editor surface on the macOS Metal pipeline.
        // Use a write-safe modality (NOT ModalityState.any()): updateFoldRegions commits
        // the document, a model change that the platform forbids from "any" contexts.
        ApplicationManager.getApplication().invokeLater({
            if (disposed) return@invokeLater
            if (detailEditor === editor && !editor.isDisposed) buildPreviewFolding(editor)
        }, ModalityState.nonModal())
    }

    private fun releaseDetailEditor() {
        detailEditor?.let { EditorFactory.getInstance().releaseEditor(it) }
        detailEditor = null
    }

    private inner class RecordRenderer : ColoredListCellRenderer<Int>() {
        override fun customizeCellRenderer(
            list: JList<out Int>, value: Int?, index: Int, selected: Boolean, hasFocus: Boolean
        ) {
            val recordIndex = value ?: return
            append("${recordIndex + 1}  ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            val preview = previewFor(recordIndex)
            val query = currentQuery
            val range = searchMatchRange(preview, query)
            if (range == null) {
                append(preview)
                // The search matches the full record text, but the preview is truncated —
                // tell the user when a listed match isn't visible in the preview itself.
                if (!query.isNullOrEmpty()) {
                    append("  (match in full text)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                return
            }
            append(preview.substring(0, range.first))
            append(
                preview.substring(range.first, range.last + 1),
                SimpleTextAttributes(SimpleTextAttributes.STYLE_SEARCH_MATCH, null)
            )
            append(preview.substring(range.last + 1))
        }
    }

    private class RangeModel(private val n: Int) : AbstractListModel<Int>() {
        override fun getSize(): Int = n
        override fun getElementAt(index: Int): Int = index
    }

    private class ArrayModel(private val a: IntArray) : AbstractListModel<Int>() {
        override fun getSize(): Int = a.size
        override fun getElementAt(index: Int): Int = a[index]
    }

    // --- FileEditor ---
    override fun getComponent(): JComponent = root
    override fun getPreferredFocusedComponent(): JComponent = recordList
    override fun getName(): String = "Records"
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = pcs.addPropertyChangeListener(listener)
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = pcs.removePropertyChangeListener(listener)
    override fun getCurrentLocation(): FileEditorLocation? = null
    override fun getFile(): VirtualFile = file
    override fun dispose() {
        disposed = true
        releaseDetailEditor()
        source?.close()
    }
}
