package com.horizons.core.diag

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import java.io.FileWriter
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Append-only startup diagnostic log. Writes to the app's external files dir
 * so the file is visible in any file manager under
 *   /sdcard/Android/data/com.horizons/files/diag/boot.log
 * No permission required (getExternalFilesDir is per-app scoped storage).
 *
 * Every major lifecycle step calls [drop] with a short tag. If the app dies
 * mid-startup, the last entry in boot.log shows exactly where.
 *
 * Also installs an UncaughtExceptionHandler that writes the full stack trace
 * to crash.log right before the process dies.
 */
object Breadcrumb {

    private const val BOOT_FILE  = "boot.log"
    private const val CRASH_FILE = "crash.log"
    private const val MAX_BOOT_SIZE_BYTES = 256L * 1024L  // 256 KiB cap
    private const val MAX_CRASH_SIZE_BYTES = 256L * 1024L // crash.log needs a cap too
    // Enough to contain the final line without reading the whole file.
    private const val TAIL_WINDOW_BYTES = 8L * 1024L

    @Volatile private var dir: File? = null
    @Volatile private var lastCrumb: String = "init"

    fun install(ctx: Context) {
        val d = ctx.getExternalFilesDir(null)?.let { File(it, "diag") }
            ?: File(ctx.filesDir, "diag")
        d.mkdirs()
        dir = d

        rotateIfTooBig(File(d, BOOT_FILE))
        // crash.log had no cap at all: every uncaught exception appended a full
        // stack trace forever. A crash-looping build grows it without bound,
        // which then makes readAll() a main-thread OOM risk.
        rotateIfTooBig(File(d, CRASH_FILE), MAX_CRASH_SIZE_BYTES)

        drop("session_start " +
            "pid=${Process.myPid()} " +
            "android=${Build.VERSION.SDK_INT} " +
            "device=${Build.MANUFACTURER}/${Build.MODEL}")

        val upstream = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                File(d, CRASH_FILE).appendText(
                    "${ts()} thread=${thread.name} last_crumb=$lastCrumb\n" +
                    throwable.stackTraceToString() +
                    "\n----\n"
                )
            }
            upstream?.uncaughtException(thread, throwable)
        }
    }

    /** Append a single breadcrumb. Cheap — no allocation beyond the line itself. */
    fun drop(tag: String) {
        lastCrumb = tag
        val d = dir ?: return
        runCatching {
            FileWriter(File(d, BOOT_FILE), true).use { w ->
                w.write("${ts()} $tag\n")
            }
        }
    }

    /** boot.log + crash.log for in-app display, tail-bounded. Called from
     *  composition on the main thread, so it must not depend on file size. */
    fun readAll(): String {
        val d = dir ?: return "(diag dir not initialized)"
        val boot = tail(File(d, BOOT_FILE), MAX_BOOT_SIZE_BYTES) ?: "(no boot.log)"
        val crash = tail(File(d, CRASH_FILE), MAX_CRASH_SIZE_BYTES) ?: ""
        return buildString {
            append("== boot.log ==\n").append(boot)
            if (crash.isNotEmpty()) append("\n== crash.log ==\n").append(crash)
        }
    }

    /** This process's most recent crumb, from memory. Never touches storage —
     *  safe on latency-critical paths like startForeground(). */
    fun lastInMemory(): String = lastCrumb

    /** Most recent breadcrumb across ALL processes — reads the boot.log tail.
     *  Use as FGS notification text so the user can see what the main process
     *  was doing right before it died, without opening the app.
     *
     *  Seeks to the end and reads a fixed window rather than scanning the file.
     *  This used to be `useLines { lastOrNull() }`, which walked all 256 KiB on
     *  the calling thread — and `buildNotification()` calls this from
     *  `onStartCommand`, inside the 10s window Android gives a foreground
     *  service to call startForeground(). On slow storage that read was enough
     *  to blow the deadline and get the process killed, and it got worse as the
     *  log grew. Keep this O(1); never let a diagnostic sit on that path. */
    fun last(): String {
        val d = dir ?: return lastCrumb
        val f = File(d, BOOT_FILE)
        if (!f.canRead()) return lastCrumb
        return runCatching {
            RandomAccessFile(f, "r").use { raf ->
                val len = raf.length()
                if (len == 0L) return@use lastCrumb
                val window = minOf(len, TAIL_WINDOW_BYTES)
                raf.seek(len - window)
                val buf = ByteArray(window.toInt())
                raf.readFully(buf)
                val text = String(buf).trimEnd('\n')
                // If the window landed mid-line and holds no separator, we
                // can't trust what we have — fall back rather than show junk.
                if (window < len && !text.contains('\n')) return@use lastCrumb
                text.substringAfterLast('\n')
                    .substringAfter(' ', missingDelimiterValue = lastCrumb)
            }
        }.getOrDefault(lastCrumb)
    }

    fun clear() {
        val d = dir ?: return
        File(d, BOOT_FILE).delete()
        File(d, CRASH_FILE).delete()
        lastCrumb = "cleared"
    }

    private fun ts(): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

    private fun rotateIfTooBig(f: File, maxBytes: Long = MAX_BOOT_SIZE_BYTES) {
        if (f.exists() && f.length() > maxBytes) {
            f.renameTo(File(f.parentFile, "${f.name}.prev"))
        }
    }

    /** Last [maxBytes] of a file, for display. Bounded so a long crash loop
     *  can't turn opening the diag pane into an OOM. */
    private fun tail(f: File, maxBytes: Long): String? {
        if (!f.canRead()) return null
        return runCatching {
            RandomAccessFile(f, "r").use { raf ->
                val len = raf.length()
                val window = minOf(len, maxBytes)
                raf.seek(len - window)
                val buf = ByteArray(window.toInt())
                raf.readFully(buf)
                val s = String(buf)
                if (window < len) "…(truncated, showing last ${window / 1024} KiB)\n$s" else s
            }
        }.getOrNull()
    }
}
