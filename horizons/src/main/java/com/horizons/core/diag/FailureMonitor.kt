package com.horizons.core.diag

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Unified, on-device failure monitor for Omni Claw.
 *
 * The app already scatters failure traces across three places:
 *   - [Breadcrumb]       -> externalFilesDir/diag/{boot.log,crash.log}
 *   - [com.horizons.core.log.CrashRecorder] -> filesDir/crashes/crash_*.txt
 *   - [com.horizons.core.log.InteractionLogger] -> filesDir/logs (JSONL, kind="error")
 *
 * FailureMonitor consolidates all of them plus any explicitly-recorded
 * failures into ONE adb-pullable location:
 *
 *   /sdcard/Android/data/com.horizons/files/failures/
 *     report.json     machine-readable snapshot (schema below)
 *     REPORT.md       human-readable rollup
 *     failures.jsonl  append-only log of explicitly-recorded failures
 *
 * externalFilesDir is per-app scoped storage: no permission needed, and any
 * CLI sandbox can pull it without root:
 *
 *   adb pull /sdcard/Android/data/com.horizons/files/failures ./failures
 *
 * That local pull is the device half. The GitHub half — how a CLI sandbox in
 * the GitHub workbench reads build/CI failures — is the committed FAILURES.md
 * ledger + the failure-monitor.yml workflow artifact. See FAILURES.md.
 *
 * report.json schema:
 *   generated_at   ISO-8601 UTC
 *   app            { package, version_name, version_code }
 *   device         { manufacturer, model, sdk_int }
 *   counts         { crashes, error_log_lines, recorded }
 *   recent_crashes [ { source, file, ts, head } ]     (most recent first, capped)
 *   recent_errors  [ { ts, backend, model, content } ] (most recent first, capped)
 *   recorded       [ { ts, tag, message, stack } ]     (most recent first, capped)
 */
object FailureMonitor {

    private const val DIR_NAME = "failures"
    private const val REPORT_JSON = "report.json"
    private const val REPORT_MD = "REPORT.md"
    private const val RECORDED_LOG = "failures.jsonl"
    private const val CAP = 30

    /** Rotate the append-only recorded log past this size so it can never grow unbounded. */
    private const val MAX_RECORDED_BYTES = 256L * 1024L

    /** Newest N log files to look at. Older ones cannot contain the most recent errors. */
    private const val MAX_LOG_FILES = 3

    @Volatile private var appContext: Context? = null

    private fun tsFormatter() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    private fun nowIso(): String = tsFormatter().format(Date())

    /** External (adb-pullable) failures dir, falling back to internal filesDir. */
    private fun dir(ctx: Context): File {
        val base = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        return File(base, DIR_NAME).apply { mkdirs() }
    }

    /**
     * Call once from Application.onCreate() after CrashRecorder.install().
     *
     * MUST STAY CHEAP. This runs on the main thread during app startup, so it does
     * NOT build a report here — it only registers the context and caps the
     * append-only log. Building the report walks every crash file and every log
     * file; doing that on the boot path meant each recorded crash made the next
     * boot slower, which is a feedback loop that manufactures the instability it
     * is supposed to be recording. The report is now built on demand only
     * ([snapshot], called from the diagnostics UI).
     */
    fun install(ctx: Context) {
        appContext = ctx.applicationContext
        runCatching { FileTail.rotateIfTooBig(File(dir(ctx), RECORDED_LOG), MAX_RECORDED_BYTES) }
    }

    /**
     * Record an explicit, caught failure that would otherwise be swallowed.
     * Safe to call from anywhere; never throws.
     */
    fun record(tag: String, message: String? = null, throwable: Throwable? = null) {
        val ctx = appContext ?: return
        runCatching {
            val line = JSONObject().apply {
                put("ts", nowIso())
                put("tag", tag)
                put("message", message ?: throwable?.message ?: "")
                // Bounded: a pathological retry loop must not be able to write an
                // unbounded stack blob on every iteration.
                put("stack", throwable?.stackTraceToString()?.take(4_000) ?: "")
            }
            val f = File(dir(ctx), RECORDED_LOG)
            FileTail.rotateIfTooBig(f, MAX_RECORDED_BYTES)
            f.appendText(line.toString() + "\n")
        }
    }

    /** Regenerate report.json + REPORT.md from all failure sources. Returns REPORT.md text. */
    fun writeReport(ctx: Context): String {
        val crashes = collectCrashes(ctx)
        val errors = collectErrorLogLines(ctx)
        val recorded = collectRecorded(ctx)

        val json = JSONObject().apply {
            put("generated_at", nowIso())
            put("app", JSONObject().apply {
                put("package", ctx.packageName)
                val pkg = runCatching {
                    ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                }.getOrNull()
                put("version_name", pkg?.versionName ?: "")
                put("version_code", pkg?.let {
                    if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong()
                } ?: 0L)
            })
            put("device", JSONObject().apply {
                put("manufacturer", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("sdk_int", Build.VERSION.SDK_INT)
            })
            put("counts", JSONObject().apply {
                put("crashes", crashes.length())
                put("error_log_lines", errors.length())
                put("recorded", recorded.length())
            })
            put("recent_crashes", crashes)
            put("recent_errors", errors)
            put("recorded", recorded)
        }

        val d = dir(ctx)
        runCatching { File(d, REPORT_JSON).writeText(json.toString(2)) }

        val md = renderMarkdown(json, crashes, errors, recorded)
        runCatching { File(d, REPORT_MD).writeText(md) }
        return md
    }

    /** Human-readable rollup for in-app display (e.g. ArtifactsPane diag view). */
    fun snapshot(): String {
        val ctx = appContext ?: return "(FailureMonitor not installed)"
        return runCatching { writeReport(ctx) }.getOrElse { "(failed to build report: ${it.message})" }
    }

    fun clear() {
        val ctx = appContext ?: return
        val d = dir(ctx)
        listOf(REPORT_JSON, REPORT_MD, RECORDED_LOG).forEach { File(d, it).delete() }
    }

    // --- collectors -------------------------------------------------------

    private fun collectCrashes(ctx: Context): JSONArray {
        val out = JSONArray()
        val entries = mutableListOf<Triple<Long, String, JSONObject>>()

        // CrashRecorder files: filesDir/crashes/crash_*.txt
        File(ctx.filesDir, "crashes").listFiles { f ->
            f.isFile && f.name.startsWith("crash_")
        }?.forEach { f ->
            entries += Triple(f.lastModified(), "crash_recorder", JSONObject().apply {
                put("source", "crash_recorder")
                put("file", f.name)
                put("ts", f.lastModified())
                put("head", f.headLines(6))
            })
        }

        // Breadcrumb crash.log: externalFilesDir/diag/crash.log (may hold multiple)
        val diagBase = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        val bc = File(File(diagBase, "diag"), "crash.log")
        if (bc.canRead()) {
            entries += Triple(bc.lastModified(), "breadcrumb", JSONObject().apply {
                put("source", "breadcrumb")
                put("file", "diag/crash.log")
                put("ts", bc.lastModified())
                put("head", bc.tailText(2048))
            })
        }

        entries.sortedByDescending { it.first }.take(CAP).forEach { out.put(it.third) }
        return out
    }

    private fun collectErrorLogLines(ctx: Context): JSONArray {
        val out = JSONArray()
        val logs = File(ctx.filesDir, "logs").listFiles { f ->
            f.isFile && f.name.endsWith(".jsonl")
        }?.sortedByDescending { it.lastModified() }?.take(MAX_LOG_FILES) ?: return out

        val collected = mutableListOf<JSONObject>()
        outer@ for (f in logs) {
            // Tail window only: we want the most recent errors, and these files are
            // append-only and unbounded. Reading them whole was O(all history).
            val lines = FileTail.lines(f, n = CAP * 20, maxBytes = FileTail.DEFAULT_WINDOW)
            for (raw in lines.asReversed()) {
                val obj = runCatching { JSONObject(raw) }.getOrNull() ?: continue
                if (obj.optString("kind") != "error") continue
                collected += JSONObject().apply {
                    put("ts", obj.optString("ts"))
                    put("backend", obj.optString("backend"))
                    put("model", obj.optString("model"))
                    put("content", obj.optString("content").ifEmpty { obj.optString("error") }.take(2000))
                }
                if (collected.size >= CAP) break@outer
            }
        }
        collected.forEach { out.put(it) }
        return out
    }

    private fun collectRecorded(ctx: Context): JSONArray {
        val out = JSONArray()
        val f = File(dir(ctx), RECORDED_LOG)
        if (!f.canRead()) return out
        val lines = FileTail.lines(f, n = CAP, maxBytes = FileTail.DEFAULT_WINDOW)
        lines.asReversed().take(CAP).forEach { raw ->
            runCatching { JSONObject(raw) }.getOrNull()?.let { out.put(it) }
        }
        return out
    }

    // --- rendering --------------------------------------------------------

    private fun renderMarkdown(
        json: JSONObject,
        crashes: JSONArray,
        errors: JSONArray,
        recorded: JSONArray,
    ): String = buildString {
        val counts = json.getJSONObject("counts")
        appendLine("# Omni Claw — On-Device Failure Report")
        appendLine()
        appendLine("- generated_at: `${json.getString("generated_at")}`")
        appendLine("- app: `${json.getJSONObject("app").optString("package")}` " +
            "v${json.getJSONObject("app").optString("version_name")}")
        appendLine("- device: `${json.getJSONObject("device").optString("manufacturer")}/" +
            "${json.getJSONObject("device").optString("model")}` " +
            "(sdk ${json.getJSONObject("device").optInt("sdk_int")})")
        appendLine("- crashes: **${counts.optInt("crashes")}** · " +
            "error log lines: **${counts.optInt("error_log_lines")}** · " +
            "recorded: **${counts.optInt("recorded")}**")
        appendLine()
        section("Recent crashes", crashes) { o ->
            "- `${o.optString("source")}` ${o.optString("file")}\n\n  ```\n  " +
                o.optString("head").trim().replace("\n", "\n  ") + "\n  ```"
        }
        section("Recent errors", errors) { o ->
            "- `${o.optString("ts")}` [${o.optString("backend")}/${o.optString("model")}] " +
                o.optString("content").replace("\n", " ").take(300)
        }
        section("Recorded failures", recorded) { o ->
            "- `${o.optString("ts")}` **${o.optString("tag")}** ${o.optString("message")}"
        }
        if (crashes.length() == 0 && errors.length() == 0 && recorded.length() == 0) {
            appendLine("_No failures recorded. Clean._")
        }
    }

    private inline fun StringBuilder.section(
        title: String,
        arr: JSONArray,
        render: (JSONObject) -> String,
    ) {
        if (arr.length() == 0) return
        appendLine("## $title (${arr.length()})")
        appendLine()
        for (i in 0 until arr.length()) {
            appendLine(render(arr.getJSONObject(i)))
        }
        appendLine()
    }

    // Bounded reads only — see FileTail for why slurping these files was the bug.
    private fun File.headLines(n: Int): String = FileTail.headLines(this, n)

    private fun File.tailText(maxBytes: Int): String = FileTail.text(this, maxBytes)
}
