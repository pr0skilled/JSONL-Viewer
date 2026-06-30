package io.jsonlviewer

import com.intellij.openapi.vfs.VirtualFile
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
 * Reads a single record's raw text from a [VirtualFile] without loading the
 * whole file. Prefers NIO random access; falls back to a skipping input stream
 * for non-local / in-archive files where [VirtualFile.toNioPath] is unsupported.
 */
class JsonlRecordReader(private val file: VirtualFile) {

    fun readRaw(index: JsonlLineIndex, i: Int): String {
        val start = index.lineStart(i)
        val rawLen = index.lineLength(i)
        val len = minOf(rawLen, MAX_RECORD_BYTES).toInt()
        val bytes = readBytes(start, len)
        var n = bytes.size
        while (n > 0 && (bytes[n - 1].toInt() == 0x0A || bytes[n - 1].toInt() == 0x0D)) n--
        val text = String(bytes, 0, n, StandardCharsets.UTF_8)
        return if (rawLen > MAX_RECORD_BYTES) "$text…" else text
    }

    private fun readBytes(start: Long, len: Int): ByteArray {
        val path: Path? = try {
            file.toNioPath()
        } catch (e: UnsupportedOperationException) {
            null
        }
        if (path != null) return readFromPath(path, start, len)
        // Fallback: stream + skip (non-local FS). Never uses contentsToByteArray().
        file.inputStream.use { ins ->
            var skipped = 0L
            while (skipped < start) {
                val s = ins.skip(start - skipped)
                if (s <= 0) break
                skipped += s
            }
            val out = ByteArray(len)
            var off = 0
            while (off < len) {
                val r = ins.read(out, off, len - off)
                if (r < 0) break
                off += r
            }
            return if (off == len) out else out.copyOf(off)
        }
    }

    companion object {
        const val MAX_RECORD_BYTES: Long = 4L * 1024 * 1024
    }
}
