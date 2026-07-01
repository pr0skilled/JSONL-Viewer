package io.jsonlviewer

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * File type for gzip-compressed JSON Lines files (`*.jsonl.gz`, `*.ndjson.gz`,
 * `*.jsonlines.gz`, `*.ldjson.gz`).
 *
 * Registered only so the IDE recognizes these files instead of prompting the
 * user for a file-type association — plain `.gz` is an unknown type in some
 * IDEs (e.g. Rider), which otherwise shows a "Register New File Type
 * Association" dialog before any editor is consulted.
 *
 * It is **binary**, so the platform never tries to load it as a text document.
 * The content itself is rendered by [JsonlRecordsEditorProvider], which
 * decompresses the file to a temp file and shows the streaming Records view
 * (its policy places that tab before the default editor).
 */
object JsonlGzFileType : FileType {
    override fun getName(): String = "Gzipped JSON Lines"
    override fun getDescription(): String = "Gzip-compressed JSON Lines (JSONL / NDJSON)"
    override fun getDefaultExtension(): String = "gz"
    override fun getIcon(): Icon = AllIcons.FileTypes.Archive
    override fun isBinary(): Boolean = true
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null
}
