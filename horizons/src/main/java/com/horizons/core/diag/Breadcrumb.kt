package com.horizons.core.diag

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process
import java.io.File
import java.io.FileWriter
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
    // crash.log used to have NO cap at all: every crash appended a full stack trace
    // forever. Combined with the boot-path readers that slurped it whole, that made
    // each crash increase the cost of the next boot. Cap it like boot.log.
    private const val MAX_CRASH_SIZE_BYTES = 128L * 1024L // 128 KiB cap

    @Volatile private var dir: File? = null
    @Volatile private var lastCrumb: String = "init"
    @Volatile private var installed = false

    fun install(ctx: Context) {
        // Installed per-process (main + :clifford). Guard so a second call in the
        // same process doesn't chain another handler onto the same file.
        if (installed) return
        installed = true

        val d = resolveDiagDir(ctx)
        dir = d

        rotateIfTooBig(File(d, BOOT_FILE))
        FileTail.rotateIfTooBig(File(d, CRASH_FILE), MAX_CRASH_SIZE_BYTES)

        drop("session_start " +
            "diag=${d.absolutePath} " +
            "pid=${Process.myPid()} " +
            "android=${Build.VERSION.SDK_INT} " +
            "device=${Build.MANUFACTURER}/${Build.MODEL}")

        val upstream = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val f = File(d, CRASH_FILE)
                FileTail.rotateIfTooBig(f, MAX_CRASH_SIZE_BYTES)
                f.appendText(
                    "${ts()} thread=${thread.name} last_crumb=$lastCrumb\n" +
                    throwable.stackTraceToString().take(16_000) +
                    "\n----\n"
                )
            }
            upstream?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Where the diagnostic logs live.
     *
     * WHY THIS IS NOT JUST getExternalFilesDir: on Android 11+ every other app —
     * Termux included — is locked out of /sdcard/Android/data/<pkg>/, and
     * MANAGE_EXTERNAL_STORAGE is explicitly carved out of that. So a log written
     * there can only be read by this app's own UI. Triage on a phone-only build
     * repeatedly stalled on exactly that: the crash trail existed and nobody
     * could get at it.
     *
     * Preference order, first writable wins:
     *   1. /sdcard/Documents/Horizons/diag  — readable by Termux and any file
     *      manager. Requires MANAGE_EXTERNAL_STORAGE, which the manifest
     *      declares; if the user has not granted it, this simply fails the
     *      write probe and we move on.
     *   2. getExternalFilesDir/diag         — app-private external, visible to
     *      this app only. The previous behaviour.
     *   3. filesDir/diag                    — internal, always writable.
     *
     * Never throws. This runs on the boot path, and a diagnostics helper that
     * can crash startup would be worse than no diagnostics at all — which is
     * the mistake this file already made once.
     */
    private fun resolveDiagDir(ctx: Context): File {
        runCatching {
            val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                Environment.isExternalStorageManager()
            if (allowed) {
                val shared = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "Horizons/diag",
                )
                if ((shared.isDirectory || shared.mkdirs()) && isWritable(shared)) return shared
            }
        }
        val fallback = ctx.getExternalFilesDir(null)?.let { File(it, "diag") }
            ?: File(ctx.filesDir, "diag")
        runCatching { fallback.mkdirs() }
        return fallback
    }

    /** Cheap write probe — mkdirs() succeeding does not prove we may write. */
    private fun isWritable(d: File): Boolean = runCatching {
        val probe = File(d, ".probe")
        if (!probe.exists()) probe.createNewFile()
        val ok = probe.canWrite()
        probe.delete()
        ok
    }.getOrDefault(false)

    /** Absolute path of the log directory, for the Artifacts pane to display. */
    fun location(): String = dir?.absolutePath ?: "(not initialised)"

    /**
     * Append a single breadcrumb. Cheap — no allocation beyond the line itself.
     *
     * Every line carries the writing process's short tag (`main` / `clifford`).
     * BOTH the main app process and the `:clifford` FGS process append to this one
     * file, and without a process tag a reader cannot tell whether `:clifford` ever
     * came up at all — which is exactly the question triage keeps getting stuck on.
     */
    fun drop(tag: String) {
        lastCrumb = tag
        val d = dir ?: return
        runCatching {
            FileWriter(File(d, BOOT_FILE), true).use { w ->
                w.write("${ts()} [${procTag()}] $tag\n")
            }
        }
    }

    /** Tail of boot.log + crash.log for in-app display. Bounded — these files
     *  are append-only, so reading them whole scaled with all past history. */
    fun readAll(): String {
        val d = dir ?: return "(diag dir not initialized)"
        val boot = FileTail.text(File(d, BOOT_FILE), 64 * 1024).ifEmpty { "(no boot.log)" }
        val crash = FileTail.text(File(d, CRASH_FILE), 64 * 1024)
        return buildString {
            // Lead with the path. When it starts with /sdcard/Documents the
            // operator can read these from Termux directly; when it starts with
            // /sdcard/Android/data they cannot, and knowing which is which has
            // cost more triage time than any bug in here.
            append("== diag dir ==\n").append(d.absolutePath).append("\n\n")
            append("== boot.log (tail) ==\n").append(boot)
            if (crash.isNotEmpty()) append("\n== crash.log (tail) ==\n").append(crash)
        }
    }

    /** Most recent breadcrumb across ALL processes — reads a bounded boot.log tail.
     *  Use as FGS notification text so the user can see what the main process
     *  was doing right before it died, without opening the app. */
    fun last(): String {
        val d = dir ?: return lastCrumb
        val f = File(d, BOOT_FILE)
        if (!f.canRead()) return lastCrumb
        return FileTail.lastLine(f)
            ?.substringAfter(' ', missingDelimiterValue = lastCrumb)
            ?: lastCrumb
    }

    /** Short tag for the current process: "main", "clifford", or the raw suffix. */
    private fun procTag(): String {
        cachedProcTag?.let { return it }
        val name = runCatching {
            File("/proc/self/cmdline").readText().trim('\u0000', ' ', '\n', '\r')
        }.getOrDefault("")
        val tag = when {
            name.isEmpty() -> "pid${Process.myPid()}"
            name.contains(':') -> name.substringAfterLast(':')
            else -> "main"
        }
        cachedProcTag = tag
        return tag
    }

    @Volatile private var cachedProcTag: String? = null

    fun clear() {
        val d = dir ?: return
        File(d, BOOT_FILE).delete()
        File(d, CRASH_FILE).delete()
        lastCrumb = "cleared"
    }

    private fun ts(): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

    private fun rotateIfTooBig(f: File) {
        if (f.exists() && f.length() > MAX_BOOT_SIZE_BYTES) {
            f.renameTo(File(f.parentFile, "${f.name}.prev"))
        }
    }
}
