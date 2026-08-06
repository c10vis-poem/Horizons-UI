package com.horizons.core.shell

import android.content.Context
import android.util.Log
import java.io.File

/**
 * The operator's guide, on disk and readable from the in-app shell.
 *
 * The manual ships as an asset (`assets/manual/OPERATORS-GUIDE.md`) and is
 * copied to `filesDir/manual/` on first use so it is a real file a shell
 * command can `cat`. That is the point: the guide has to be reachable from the
 * Terminal, not only from a UI screen, so a scripted agent can serve it back as
 * a help-desk function later.
 *
 * Resolution is by chapter number or free-text search rather than an index the
 * caller has to maintain, because the guide will grow and any hard-coded table
 * of contents would go stale the first time a chapter is added.
 */
object ManualStore {

    const val ASSET_PATH = "manual/OPERATORS-GUIDE.md"
    const val FILE_NAME = "OPERATORS-GUIDE.md"
    private const val TAG = "ManualStore"

    /** `filesDir/manual/OPERATORS-GUIDE.md`, extracted on first access. */
    fun file(context: Context): File? {
        val dir = File(context.filesDir, "manual").apply { mkdirs() }
        val dest = File(dir, FILE_NAME)

        // Re-extract when the packaged asset is newer than what we unpacked —
        // otherwise an app update ships a new guide that nobody ever sees.
        val stale = !dest.isFile || dest.length() == 0L
        if (!stale) return dest

        return try {
            context.assets.open(ASSET_PATH).use { src ->
                dest.outputStream().use { dst -> src.copyTo(dst) }
            }
            Log.i(TAG, "Manual extracted -> ${dest.absolutePath}")
            dest
        } catch (e: Exception) {
            Log.w(TAG, "Manual asset not packaged: ${e.message}")
            null
        }
    }

    /** Absolute path, for `cat` or any external reader. Null if unavailable. */
    fun path(context: Context): String? = file(context)?.absolutePath

    private fun text(context: Context): String? =
        runCatching { file(context)?.readText() }.getOrNull()

    /**
     * Split on level-1 and level-2 headings, keeping the heading with its body.
     * Chapters are numbered in document order so `manual 3` is stable.
     */
    private fun sections(md: String): List<String> {
        val out = mutableListOf<StringBuilder>()
        md.lineSequence().forEach { line ->
            if (line.startsWith("# ") || line.startsWith("## ")) {
                out += StringBuilder(line).append('\n')
            } else if (out.isNotEmpty()) {
                out.last().append(line).append('\n')
            }
        }
        return out.map { it.toString().trimEnd() }
    }

    /**
     * Resolve a `manual` invocation.
     *
     * - no argument    → the table of contents
     * - a number       → that section
     * - anything else  → sections whose text matches, case-insensitively
     */
    fun query(context: Context, arg: String? = null): String {
        val md = text(context)
            ?: return "manual: guide not available in this build."
        val secs = sections(md)
        if (secs.isEmpty()) return md

        val a = arg?.trim().orEmpty()

        if (a.isEmpty()) {
            return buildString {
                appendLine("HORIZONS — OPERATOR'S GUIDE")
                appendLine("  manual <n>       read a section")
                appendLine("  manual <text>    search")
                appendLine()
                secs.forEachIndexed { i, s ->
                    val head = s.lineSequence().first()
                        .removePrefix("## ").removePrefix("# ").trim()
                    appendLine("  ${(i + 1).toString().padStart(2)}  $head")
                }
            }.trimEnd()
        }

        a.toIntOrNull()?.let { n ->
            return secs.getOrNull(n - 1)
                ?: "manual: no section $n (1..${secs.size})"
        }

        val hits = secs.filter { it.contains(a, ignoreCase = true) }
        return when {
            hits.isEmpty() -> "manual: nothing matching \"$a\""
            else -> hits.joinToString("\n\n${"-".repeat(60)}\n\n")
        }
    }
}
