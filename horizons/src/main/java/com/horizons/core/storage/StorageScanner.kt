package com.horizons.core.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Walks common device storage roots and returns model / runtime artifacts.
 *
 * Requires MANAGE_EXTERNAL_STORAGE (already declared in the manifest, granted
 * via SettingsPane). Without that grant, [scan] returns an empty list — the
 * caller checks [canScan] first and prompts the operator to grant it.
 *
 * The point of this class: the operator's GenieX SDK, Qwen models, shards
 * and .so libraries sit in /storage/emulated/0/LeGRAND_REPOSITORY/ (or wherever
 * the file manager dropped them). The app previously only saw files if you
 * shared them one at a time through "Open with Horizons" — which meant the
 * operator watched the app fail to find backend files it could physically
 * see in a file manager on the same device. This closes that gap.
 */
object StorageScanner {

    fun canScan(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true

    /**
     * Recursively walks the common roots. Depth-limited so a rogue
     * symlink or an SD-card game cache can't hang the UI.
     */
    suspend fun scan(): List<Group> = withContext(Dispatchers.IO) {
        if (!canScan()) return@withContext emptyList()

        val root = Environment.getExternalStorageDirectory()
        val seeds = mutableListOf<File>()
        // Explicit candidates operator has actually used
        seeds += File(root, "LeGRAND_REPOSITORY")
        seeds += File(root, "Download")
        seeds += File(root, "Documents")
        // .horizons/ (if the operator dropped files under a hidden folder)
        seeds += File(root, ".horizons")
        // Fallback root itself, but only one level deep — so we surface
        // top-level model folders without descending into every DCIM album.
        seeds += root

        val hits = mutableListOf<FoundFile>()
        seeds.filter { it.exists() && it.canRead() }.forEach { seed ->
            val maxDepth = if (seed == root) 1 else 6
            walk(seed, 0, maxDepth, hits)
        }

        hits.groupBy { it.parentDir }
            .map { (dir, files) -> Group(dir, files.sortedBy { it.name }) }
            .sortedBy { it.dir }
    }

    private fun walk(dir: File, depth: Int, maxDepth: Int, out: MutableList<FoundFile>) {
        if (depth > maxDepth) return
        val entries = dir.listFiles() ?: return
        for (entry in entries) {
            if (entry.isDirectory) {
                // Skip Android app-private storage — nothing interesting there and
                // it takes forever to walk.
                if (entry.name == "Android" && depth == 0) continue
                walk(entry, depth + 1, maxDepth, out)
            } else if (entry.isFile && entry.canRead()) {
                val kind = classify(entry.name) ?: continue
                out += FoundFile(
                    name = entry.name,
                    absolutePath = entry.absolutePath,
                    parentDir = entry.parentFile?.absolutePath ?: "",
                    sizeBytes = entry.length(),
                    kind = kind,
                )
            }
        }
    }

    private fun classify(name: String): Kind? {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".gguf") -> Kind.GGUF
            lower.endsWith(".onnx") -> Kind.ONNX
            lower.endsWith(".bin") || lower.endsWith(".serialized.bin") -> Kind.BIN
            lower.endsWith(".tflite") -> Kind.TFLITE
            lower.endsWith(".dlc") -> Kind.DLC
            lower.endsWith(".pte") -> Kind.PTE
            lower.endsWith(".qnn") -> Kind.QNN
            lower.endsWith(".so") || lower.contains(".so.") -> Kind.LIB
            lower == "geniex.json" -> Kind.GENIEX_MANIFEST
            lower == "voices.bin" -> Kind.VOICES
            lower == "tokens.txt" -> Kind.TOKENS
            lower.contains("encoder") && (lower.endsWith(".onnx") || lower.endsWith(".int8.onnx")) -> Kind.ONNX
            lower.contains("decoder") && (lower.endsWith(".onnx") || lower.endsWith(".int8.onnx")) -> Kind.ONNX
            // Extensionless executables — geniex, llama-server, ort_engine, etc.
            !lower.contains('.') && name.length in 2..40 -> Kind.BINARY
            else -> null
        }
    }

    /**
     * Copies [source] into the app's models directory (models/) for model files
     * or the app's files directory for binaries/libs. Returns the destination
     * path on success, or null on failure. Runs on IO.
     */
    suspend fun importInto(ctx: Context, source: FoundFile): String? = withContext(Dispatchers.IO) {
        val src = File(source.absolutePath)
        if (!src.canRead()) return@withContext null

        val destDir = when (source.kind) {
            Kind.LIB, Kind.BINARY -> ctx.filesDir
            else -> File(ctx.filesDir, "models").apply { mkdirs() }
        }
        val dest = File(destDir, source.name)
        try {
            FileInputStream(src).use { input ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(256 * 1024)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) out.write(buf, 0, read)
                }
            }
            if (source.kind == Kind.BINARY) dest.setExecutable(true, true)
            dest.absolutePath
        } catch (_: Throwable) {
            null
        }
    }

    enum class Kind(val label: String) {
        GGUF("GGUF"),
        ONNX("ONNX"),
        BIN("BIN"),
        TFLITE("TFLITE"),
        DLC("DLC"),
        PTE("PTE"),
        QNN("QNN"),
        LIB("SO"),
        BINARY("EXE"),
        GENIEX_MANIFEST("GENIEX"),
        VOICES("VOICES"),
        TOKENS("TOKENS"),
    }

    data class FoundFile(
        val name: String,
        val absolutePath: String,
        val parentDir: String,
        val sizeBytes: Long,
        val kind: Kind,
    ) {
        val sizeMb: Long get() = sizeBytes / (1024 * 1024)
    }

    data class Group(val dir: String, val files: List<FoundFile>)
}
