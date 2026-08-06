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
            RuntimeDef(
                id = "llama000",
                name = "llama-server",
                binaryName = "llama-server",
                port = 8081,
                healthPath = "/health",
                argsTemplate = "--model {model} --port {port} --host 127.0.0.1",
                requiredAssets = emptyList(),
                notes = "llama.cpp server. GGUF on CPU — no NPU/QNN assets, so it runs on " +
                    "any arm64 device. Import the binary via Open with -> Horizons: the " +
                    "launcher execs it from filesDir and cannot reach Termux's own " +
                    "directory. Binds loopback, which Termux shares, so a Termux agent " +
                    "reaches it at 127.0.0.1:8081 (OpenAI-compatible wire on /v1).",
                builtIn = true,
            ),
        )
    }
}

/** One green light on the fuse box panel. */
data class AssetCheck(val label: String, val ok: Boolean, val detail: String)

val List<AssetCheck>.allGreen: Boolean get() = isNotEmpty() && all { it.ok }

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

    return checks
}

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
