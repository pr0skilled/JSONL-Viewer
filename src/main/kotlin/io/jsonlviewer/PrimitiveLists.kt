package io.jsonlviewer

/** Minimal growable primitive lists — avoids boxing and any external dependency. */
internal class LongList(initial: Int = 1024) {
    private var data = LongArray(initial.coerceAtLeast(4))
    var size = 0; private set
    fun add(v: Long) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = v
    }
    fun toArray(): LongArray = data.copyOf(size)
    /** Returns a copy of the contents with [extra] appended as the final element. */
    fun toArrayWith(extra: Long): LongArray {
        val r = data.copyOf(size + 1); r[size] = extra; return r
    }
}

internal class IntList(initial: Int = 256) {
    private var data = IntArray(initial.coerceAtLeast(4))
    var size = 0; private set
    fun add(v: Int) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = v
    }
    fun clear() { size = 0 }
    fun toArray(): IntArray = data.copyOf(size)
}
