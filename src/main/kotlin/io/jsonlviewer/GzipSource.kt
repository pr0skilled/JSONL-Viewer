package io.jsonlviewer

import com.intellij.openapi.progress.ProgressIndicator
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream

private const val BUFFER = 1 shl 16

/**
 * Decompresses a gzip [compressed] stream into a fresh temp file and returns its
 * path. Reuses the offset-based reader/index/search against a real filesystem
 * path — gzip itself is not randomly seekable. The temp file is registered for
 * delete-on-exit as a crash backstop; the caller (a [TempFileSource]) deletes it
 * on dispose. On any failure the partial temp file is removed and the exception
 * is rethrown.
 *
 * [compressedLength] (the .gz file size) drives the progress indicator; pass 0
 * for an indeterminate bar (e.g. in tests).
 */
fun decompressToTempFile(
    compressed: InputStream,
    indicator: ProgressIndicator?,
    compressedLength: Long
): Path {
    val tempPath = Files.createTempFile("jsonl-viewer-", ".jsonl")
    tempPath.toFile().deleteOnExit()
    try {
        val counting = CountingInputStream(compressed)
        GZIPInputStream(counting).use { gz ->
            Files.newOutputStream(tempPath).buffered().use { out ->
                val buf = ByteArray(BUFFER)
                while (true) {
                    indicator?.checkCanceled()
                    val r = gz.read(buf)
                    if (r < 0) break
                    out.write(buf, 0, r)
                    if (compressedLength > 0 && indicator != null) {
                        indicator.fraction = (counting.count.toDouble() / compressedLength).coerceIn(0.0, 1.0)
                    }
                }
            }
        }
    } catch (e: Throwable) {
        Files.deleteIfExists(tempPath)
        throw e
    }
    return tempPath
}

/** Counts bytes read from [delegate] so decompression progress can be reported. */
private class CountingInputStream(private val delegate: InputStream) : InputStream() {
    var count: Long = 0L
        private set

    override fun read(): Int {
        val b = delegate.read()
        if (b >= 0) count++
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val r = delegate.read(b, off, len)
        if (r > 0) count += r
        return r
    }

    override fun close() = delegate.close()
}
