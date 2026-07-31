package com.horizons.core.diag

import java.io.File
import java.io.RandomAccessFile

/**
 * Bounded file reads for the diagnostic layer.
 *
 * WHY THIS EXISTS: every "read the tail of the log" call in the diag code used to
 * be `readText().takeLast(n)` or `readLines().asReversed().take(n)` — both of which
 * load the ENTIRE file into memory before throwing almost all of it away. On the
 * boot path, against append-only logs that grow with every crash, that created a
 * positive feedback loop: each crash made the next boot's read bigger and slower,
 * so the diagnostics actively made the instability they were meant to record worse.
 *
 * These helpers seek instead of slurping. Cost is O(window), not O(file).
 */
internal object FileTail {

    /**
     * Last [maxBytes] bytes of [file] as text, without reading the rest.
     *
     * A partial multi-byte UTF-8 sequence at the cut point is dropped by starting
     * at the first newline inside the window (falling back to the raw window when
     * the window holds no newline).
     */
    fun text(file: File, maxBytes: Int): String = runCatching {
        if (!file.canRead()) return ""
        val len = file.length()
        if (len <= 0L) return ""
        if (len <= maxBytes) return file.readText()

        val buf = ByteArray(maxBytes)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(len - maxBytes)
            raf.readFully(buf)
        }
        val s = String(buf, Charsets.UTF_8)
        val nl = s.indexOf('\n')
        if (nl >= 0 && nl < s.length - 1) s.substring(nl + 1) else s
    }.getOrDefault("")

    /** Last [n] non-blank lines of [file], reading at most [maxBytes] from the end. */
    fun lines(file: File, n: Int, maxBytes: Int = DEFAULT_WINDOW): List<String> =
        text(file, maxBytes)
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()
            .takeLast(n)

    /** Last non-blank line of [file], or null. Reads only the tail window. */
    fun lastLine(file: File, maxBytes: Int = SMALL_WINDOW): String? =
        lines(file, 1, maxBytes).firstOrNull()

    /** First [n] lines of [file], streamed — never loads the whole file. */
    fun headLines(file: File, n: Int): String = runCatching {
        if (!file.canRead()) return ""
        file.useLines { seq -> seq.take(n).joinToString("\n") }
    }.getOrDefault("")

    /**
     * Keep [file] under [maxBytes] by rotating it to `<name>.prev` (replacing any
     * previous rotation). Bounds total on-disk cost at 2x [maxBytes] instead of
     * letting an append-only log grow without limit.
     */
    fun rotateIfTooBig(file: File, maxBytes: Long) {
        runCatching {
            if (!file.exists() || file.length() <= maxBytes) return
            val prev = File(file.parentFile, "${file.name}.prev")
            prev.delete()
            if (!file.renameTo(prev)) file.delete()
        }
    }

    const val SMALL_WINDOW = 8 * 1024
    const val DEFAULT_WINDOW = 64 * 1024
}
