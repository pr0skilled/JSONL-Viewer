package io.jsonlviewer

import com.intellij.openapi.vfs.VirtualFile
import java.io.Closeable
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * A source of decompressed record bytes. Hides whether the bytes live in the
 * original file (plain JSONL) or in a temp file we decompressed a gzip into.
 *
 * [openStream] yields a fresh stream over the whole (decompressed) content from
 * byte 0 — used by indexing and search. [read] gives random access by byte
 * offset into the same decompressed content — used to load one record on demand.
 */
interface RecordSource : Closeable {
    fun openStream(): InputStream
    val length: Long
    fun read(start: Long, len: Int): ByteArray
}

/** Backs a plain (uncompressed) JSON Lines file directly by its [VirtualFile]. */
class VirtualFileSource(private val file: VirtualFile) : RecordSource {
    override fun openStream(): InputStream = file.inputStream

    override val length: Long
        get() = file.length

    override fun read(start: Long, len: Int): ByteArray {
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

    override fun close() {} // owns no resources
}

/** Backs a JSON Lines file we decompressed a gzip into; [close] deletes the temp file. */
class TempFileSource(private val path: Path, override val length: Long) : RecordSource {
    override fun openStream(): InputStream = Files.newInputStream(path)

    override fun read(start: Long, len: Int): ByteArray = readFromPath(path, start, len)

    override fun close() {
        Files.deleteIfExists(path)
    }
}
