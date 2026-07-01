package io.jsonlviewer

import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Pure helper: read exactly [len] bytes starting at [start] from [path]. */
internal fun readFromPath(path: Path, start: Long, len: Int): ByteArray {
    RandomAccessFile(path.toFile(), "r").use { raf ->
        raf.seek(start)
        val out = ByteArray(len)
        var off = 0
        while (off < len) {
            val r = raf.read(out, off, len - off)
            if (r < 0) break
            off += r
        }
        return if (off == len) out else out.copyOf(off)
    }
}

/**
 * Reads a single record's raw text from a [RecordSource] without loading the
 * whole file. The source decides how bytes are fetched (random access on a real
 * path, or a skipping stream fallback).
 */
class JsonlRecordReader(private val source: RecordSource) {

    fun readRaw(index: JsonlLineIndex, i: Int): String {
        val start = index.lineStart(i)
        val rawLen = index.lineLength(i)
        val len = minOf(rawLen, MAX_RECORD_BYTES).toInt()
        val bytes = source.read(start, len)
        var n = bytes.size
        while (n > 0 && (bytes[n - 1].toInt() == 0x0A || bytes[n - 1].toInt() == 0x0D)) n--
        val text = String(bytes, 0, n, StandardCharsets.UTF_8)
        return if (rawLen > MAX_RECORD_BYTES) "$text…" else text
    }

    companion object {
        const val MAX_RECORD_BYTES: Long = 4L * 1024 * 1024
    }
}
