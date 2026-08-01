package com.horizons.core.state

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Runtime definitions — authored in the Terminal (the mod garage), shipped
 * to the Monitor (the console) for acknowledgment and green-light checks.
 *
 * A definition is pure parameters: what binary, what port, what handshake
 * (health endpoint), what assets must be present. Defining one launches
 * nothing and loads nothing — the Monitor validates the assets and shows
 * per-file green lights; actually running a runtime stays a separate,
 * user-paced step. This is the "handshake/permissions first, acquire
 * assets second, green light third" pipeline.
 */
data class RuntimeDef(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val name: String,
    val binaryName: String,
    val port: Int,
    val healthPath: String = "/health",
    val argsTemplate: String = "",
    val requiredAssets: List<String> = emptyList(),
    val notes: String = "",
    val builtIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("binaryName", binaryName)
        put("port", port)
        put("healthPath", healthPath)
        put("argsTemplate", argsTemplate)
        put("requiredAssets", JSONArray(requiredAssets))
        put("notes", notes)
        put("builtIn", builtIn)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): RuntimeDef = RuntimeDef(
            id = obj.getString("id"),
            name = obj.getString("name"),
            binaryName = obj.getString("binaryName"),
            port = obj.getInt("port"),
            healthPath = obj.optString("healthPath", "/health"),
            argsTemplate = obj.optString("argsTemplate", ""),
            requiredAssets = obj.optJSONArray("requiredAssets")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            notes = obj.optString("notes", ""),
            builtIn = obj.optBoolean("builtIn", false),
            createdAt = obj.optLong("createdAt", 0L),
        )

        /** The two runtimes the project already knows about, pre-defined. */
        fun builtIns(): List<RuntimeDef> = listOf(
            RuntimeDef(
                id = "ort0000",
                name = "ort_engine",
                binaryName = "ort_engine",
                port = 8080,
                healthPath = "/health",
                argsTemplate = "--model {model} --port {port}",
                requiredAssets = listOf(
                    "libonnxruntime.so",
                    "libQnnHtp.so",
                    "libQnnSystem.so",
                    "libQnnHtpV79Skel.so",
                ),
                notes = "Legacy ORT+QNN daemon, CI-built. Wire: POST /api/v1/generate.",
                builtIn = true,
            ),
            RuntimeDef(
                id = "genx0000",
                name = "geniex",
                binaryName = "geniex",
                port = 18181,
                healthPath = "/v1/models",
                argsTemplate = "serve --model {model} --port {port}",
                requiredAssets = emptyList(),
                notes = "GenieX GGML/QAIRT daemon (planned). OpenAI-compatible wire on :18181/v1.",
                builtIn = true,
            ),
        )
    }
}

/**
 * One green light on the fuse box panel.
 *
 * [advisory] marks a check that REPORTS but never REFUSES. The operator's rule:
 * "the router doesn't need to think about saying yes or no, it just does — it's
 * going to try to connect whatever you put on there." The master doc is explicit
 * that the hard red banner refusing to turn on gets stripped out. So a check
 * that can't be satisfied by editing the config — you cannot make a 5.4 GB model
 * smaller from this screen — must not block the switch. Tell the operator the
 * numbers and let them decide. Inventing a gotcha and then enforcing it against
 * ourselves is not a feature.
 */
data class AssetCheck(
    val label: String,
    val ok: Boolean,
    val detail: String,
    val advisory: Boolean = false,
)

/** Advisory checks colour the panel but never hold the switch shut. */
val List<AssetCheck>.allGreen: Boolean get() = isNotEmpty() && all { it.ok || it.advisory }

/**
 * Static green-light check — file presence/readability/exec bit only, no
 * network, no side effects. The Monitor renders these lights; the Router
 * refuses to switch a config on unless every one is green.
 */
fun RuntimeDef.greenLight(context: Context, modelPath: String?): List<AssetCheck> {
    val candidateDirs = listOf(
        context.filesDir,
        File(context.filesDir, "models"),
        File(context.applicationInfo.nativeLibraryDir),
        File("/storage/emulated/0/Download"),
    )

    fun find(name: String): File? =
        candidateDirs.asSequence().map { File(it, name) }.firstOrNull { it.canRead() }

    val checks = mutableListOf<AssetCheck>()

    val bin = find(binaryName)
    checks += AssetCheck(
        "binary $binaryName",
        bin != null,
        bin?.absolutePath ?: "not found in app dirs or Download",
    )
    if (bin != null) {
        checks += AssetCheck(
            "exec permission",
            bin.canExecute(),
            if (bin.canExecute()) "ok" else "needs chmod +x (relaunch import)",
        )
    }

    requiredAssets.forEach { asset ->
        val f = find(asset)
        checks += AssetCheck(
            "asset $asset",
            f != null,
            f?.absolutePath ?: "not found",
        )
    }

    if (argsTemplate.contains("{model}")) {
        val ok = modelPath != null && File(modelPath).canRead()
        checks += AssetCheck(
            "model plugged in",
            ok,
            modelPath ?: "no model plugged in — flip the switch in Monitor",
        )
    }

    checks += roadAndWeightLimit(context, modelPath)

    return checks
}

/**
 * Parameter #3 of the operator's four: THE ROAD & WEIGHT LIMIT.
 *
 * "Can this engine actually survive on this device? Is the model too heavy
 *  (low memory/RAM), or is the program incompatible with the phone's
 *  architecture? If the vehicle is too big for the road, it's going to crash
 *  before it even gets going."
 *
 * This is the amperage limit, and per the master doc it exists SPECIFICALLY to
 * stop OOM crashes. greenLight() shipped without it for a long time, which is
 * how a config could be ALL GREEN and still be killed by Android's low-memory
 * killer seconds after loading — a death that leaves no stack trace, so it
 * reads as a mystery crash rather than a capacity problem.
 *
 * Deliberately static: reads ActivityManager's memory snapshot and the model's
 * size on disk. No network, no side effects, consistent with every other check
 * here.
 */
private fun roadAndWeightLimit(context: Context, modelPath: String?): List<AssetCheck> {
    val out = mutableListOf<AssetCheck>()

    // Architecture. The daemons and the QNN/HTP skels are arm64-v8a only.
    val abis = android.os.Build.SUPPORTED_ABIS?.toList().orEmpty()
    out += AssetCheck(
        "architecture arm64-v8a",
        abis.contains("arm64-v8a"),
        if (abis.contains("arm64-v8a")) abis.joinToString() else "device reports ${abis.joinToString()}",
        advisory = true,
    )

    // Weight vs available memory. Only meaningful once a model is plugged in.
    val f = modelPath?.let { File(it) }?.takeIf { it.canRead() } ?: run {
        out += AssetCheck("weight limit", true, "no model plugged in — nothing to weigh yet", advisory = true)
        return out
    }

    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
    val mi = android.app.ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
    val modelBytes = f.length()
    val avail = mi.availMem
    val needed = modelBytes + RUNTIME_HEADROOM_BYTES

    fun gb(b: Long) = String.format(java.util.Locale.US, "%.2f GB", b / 1_073_741_824.0)

    out += AssetCheck(
        "weight limit",
        needed <= avail && !mi.lowMemory,
        when {
            mi.lowMemory -> "device is in a low-memory state (avail ${gb(avail)}) — free memory first"
            needed > avail ->
                "model ${gb(modelBytes)} + ${gb(RUNTIME_HEADROOM_BYTES)} runtime > ${gb(avail)} available"
            else -> "model ${gb(modelBytes)}, ${gb(avail)} available"
        },
        advisory = true,
    )

    return out
}

/**
 * Headroom reserved on top of the PRIMARY model's own bytes.
 *
 * This is not "a bit of slack for the runtime". The target deployment is the
 * dual-agent pair, both resident at once:
 *
 *   - executive model — Qwen 3.5 9B Q4_0, ~5.4 GB
 *   - query model     — Qwen 3.5 0.8B, always on, the one that decides whether
 *                       the executive gets woken at all
 *   - KV cache for both, the ggml/HTP runtime, and the app itself
 *
 * The operator's figures, which this constant is derived from and not the other
 * way round:
 *   - Qwen pair (0.8B query + 9B executive) WEIGHTS together: ~6.95 GB
 *   - Gemma set: ~7.21 GB
 *   both sit inside the same envelope, so the budget must cover ~7.2 GB of
 *   weights, not just the pinned primary
 *   - the device routinely has 10-12 GB free
 *   - any cap under ~9-9.5 GB total is not feasible for this stack
 *
 * So with a 5.4 GB primary pinned, the companion weights account for ~1.55 GB of
 * headroom, and KV cache + runtime + app take the remainder up to the ~9-9.5 GB
 * envelope. 4.0 GB over the primary lands at ~9.4 GB total.
 *
 * This is still a constant standing in for something the app should MEASURE —
 * sum what is actually plugged in against real availMem — which is the
 * operator's call and is recorded in CLAUDE.md. Do not treat the number as
 * settled design; treat it as a placeholder with its arithmetic shown so it can
 * be checked.
 *
 * An earlier value of 768 MB was set before the companion model was taken into
 * account and was far too low — it would have flagged a perfectly runnable
 * config as over-weight. Since this check is ADVISORY it could never have
 * blocked anything, but a wrong number that merely misinforms is still wrong.
 */
private const val RUNTIME_HEADROOM_BYTES = 4096L * 1024L * 1024L

class RuntimeDefStore(context: Context) {

    private val file = File(context.filesDir, "runtime_defs.json")

    private val _defs = MutableStateFlow<List<RuntimeDef>>(emptyList())
    val defs: StateFlow<List<RuntimeDef>> = _defs.asStateFlow()

    init { reload() }

    fun reload() {
        val stored = if (file.exists()) {
            try {
                val arr = JSONArray(file.readText())
                (0 until arr.length()).map { RuntimeDef.fromJson(arr.getJSONObject(it)) }
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()
        // Built-ins always present; user definitions layer on top.
        val userDefs = stored.filter { !it.builtIn }
        _defs.value = RuntimeDef.builtIns() + userDefs
    }

    fun add(def: RuntimeDef) {
        _defs.value = _defs.value + def
        persist()
    }

    fun remove(id: String) {
        // Built-ins can't be removed — they document what the app ships with.
        _defs.value = _defs.value.filterNot { it.id == id && !it.builtIn }
        persist()
    }

    private fun persist() {
        try {
            val arr = JSONArray()
            _defs.value.filter { !it.builtIn }.forEach { arr.put(it.toJson()) }
            file.writeText(arr.toString(2))
        } catch (_: Exception) {
        }
    }
}
