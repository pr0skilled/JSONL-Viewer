package io.jsonlviewer

import com.intellij.openapi.progress.ProgressIndicator
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Streams the file once and returns indices of records whose raw text contains
 * [query] (case-insensitive). Cancellable; reports progress; calls [onBatch] with
 * newly matched indices periodically so the UI can update incrementally.
 *
 * Lines are split on the raw 0x0A byte (a record boundary — see [JsonlLineIndex]),
 * accumulated into a buffer and decoded as UTF-8, so multi-byte characters that
 * straddle the read buffer are handled correctly. Record numbering matches the index.
 */
object JsonlSearch {
    private const val BUFFER = 1 shl 16
    private const val BATCH = 256

    fun find(
        input: InputStream,
        index: JsonlLineIndex,
        query: String,
        fileLength: Long,
        indicator: ProgressIndicator? = null,
        onBatch: (IntArray) -> Unit = {}
    ): IntArray {
        if (query.isEmpty() || index.count == 0) return IntArray(0)
        val needle = query.lowercase()
        val result = IntList()
        val batch = IntList()
        val lineBytes = ByteArrayOutputStream(256)
        val buf = ByteArray(BUFFER)
        var rec = 0
        var pos = 0L

        fun flushLine() {
            val text = String(lineBytes.toByteArray(), StandardCharsets.UTF_8)
            if (text.lowercase().contains(needle)) {
                result.add(rec); batch.add(rec)
                if (batch.size >= BATCH) { onBatch(batch.toArray()); batch.clear() }
            }
            lineBytes.reset()
            rec++
        }

        while (true) {
            indicator?.checkCanceled()
            val read = input.read(buf)
            if (read < 0) break
            for (k in 0 until read) {
                pos++
                val b = buf[k]
                if (b.toInt() == 0x0A) flushLine() else lineBytes.write(b.toInt())
            }
            if (fileLength > 0) indicator?.fraction = (pos.toDouble() / fileLength).coerceIn(0.0, 1.0)
        }
        if (lineBytes.size() > 0) flushLine() // final line without a trailing newline
        if (batch.size > 0) onBatch(batch.toArray())
        return result.toArray()
    }
}
