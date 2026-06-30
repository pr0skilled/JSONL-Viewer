package io.jsonlviewer

import com.intellij.openapi.progress.ProgressIndicator
import java.io.InputStream

/**
 * Byte offsets of every record (line) start, built by a single streamed pass.
 * Holds `count + 1` offsets where `offsets[count]` is the end-of-file offset,
 * so record `i` occupies bytes `[offsets[i], offsets[i + 1])`.
 *
 * UTF-8 safe: a `0x0A` byte never appears inside a multi-byte sequence, and JSON
 * forbids raw newlines inside strings, so `0x0A` marks exactly the record breaks.
 */
class JsonlLineIndex private constructor(
    private val offsets: LongArray,
    val count: Int
) {
    fun lineStart(i: Int): Long = offsets[i]
    fun lineEnd(i: Int): Long = offsets[i + 1]
    fun lineLength(i: Int): Long = offsets[i + 1] - offsets[i]

    companion object {
        private const val BUFFER = 1 shl 16

        fun build(input: InputStream, fileLength: Long, indicator: ProgressIndicator? = null): JsonlLineIndex {
            val starts = LongList()
            starts.add(0L) // record 0 starts at byte 0
            val buf = ByteArray(BUFFER)
            var pos = 0L
            var lastWasNewline = false
            while (true) {
                indicator?.checkCanceled()
                val read = input.read(buf)
                if (read < 0) break
                for (k in 0 until read) {
                    pos++
                    val isNewline = buf[k].toInt() == 0x0A
                    if (isNewline) starts.add(pos) // next record starts after the newline
                    lastWasNewline = isNewline
                }
                if (fileLength > 0) indicator?.fraction = (pos.toDouble() / fileLength).coerceIn(0.0, 1.0)
            }
            val end = pos
            if (end == 0L) return JsonlLineIndex(longArrayOf(0L), 0)
            // `starts` = [0, then one offset after each newline].
            return if (lastWasNewline) {
                // The final pushed offset == end and denotes the empty trailing line,
                // which is not a record — keep it only as the end sentinel.
                JsonlLineIndex(starts.toArray(), starts.size - 1)
            } else {
                // File did not end with a newline: append the end sentinel.
                JsonlLineIndex(starts.toArrayWith(end), starts.size)
            }
        }

        fun build(bytes: ByteArray): JsonlLineIndex =
            build(bytes.inputStream(), bytes.size.toLong(), null)
    }
}
