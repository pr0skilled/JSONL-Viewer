package io.jsonlviewer

import com.intellij.openapi.vfs.VirtualFile

/**
 * Single source of truth for recognizing the JSON Lines files this plugin views,
 * plain or gzip-compressed. Detection is name-based: `data.jsonl.gz` is accepted
 * (inner extension is a JSON Lines variant) while `archive.gz` / `notes.txt.gz`
 * are not, so we never hijack arbitrary gzip files.
 */
object JsonlFormat {
    private val JSONL_EXTS = setOf("jsonl", "ndjson", "jsonlines", "ldjson")

    fun accepts(file: VirtualFile): Boolean = acceptsName(file.name)

    fun isGzip(file: VirtualFile): Boolean = isGzipName(file.name)

    fun acceptsName(name: String): Boolean {
        val lower = name.lowercase()
        val ext = lower.substringAfterLast('.', "")
        if (ext in JSONL_EXTS) return true
        return ext == "gz" && innerExt(lower) in JSONL_EXTS
    }

    fun isGzipName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.substringAfterLast('.', "") == "gz" && innerExt(lower) in JSONL_EXTS
    }

    /** Extension underneath a trailing `.gz`, e.g. "data.jsonl.gz" -> "jsonl". */
    private fun innerExt(lowerName: String): String =
        lowerName.removeSuffix(".gz").substringAfterLast('.', "")
}
