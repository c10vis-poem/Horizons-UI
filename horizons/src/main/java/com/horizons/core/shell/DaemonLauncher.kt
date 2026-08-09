package com.horizons.core.shell

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Launches the native ONNX-RT/QNN engine binary as a detached background daemon.
 *
 * Expected binary location: context.filesDir/ort_engine
 * Deploy via: adb push ort_engine /data/data/com.horizons/files/ort_engine
 *             adb shell chmod +x /data/data/com.horizons/files/ort_engine
 *
 * The daemon serves inference requests at 127.0.0.1:8080 (see NpuClient).
 *
 * Priority: If root is available, oom_score_adj is set to -1000 (unkillable by lmkd).
 * Without root, the daemon inherits the app's oom_score_adj and may be killed under
 * extreme memory pressure. The ADB shell -950 loophole documented in ADB_CLOUD_BUILD
 * requires Wireless Debugging; this launcher uses sh -T- detach instead.
 */
class DaemonLauncher(
    private val context: Context,
    private val binaryName: String = ENGINE_BINARY,
) {

    data class DaemonHandle(val pid: Int, val logPath: String)

    suspend fun launch(engineArgs: List<String> = emptyList()): Result<DaemonHandle> =
        withContext(Dispatchers.IO) {
            val engine = File(context.filesDir, binaryName)
            if (!engine.exists()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Engine binary not found: ${engine.absolutePath}\n" +
                        "Deploy: adb push $binaryName ${engine.absolutePath} && " +
                        "adb shell chmod +x ${engine.absolutePath}"
                    )
                )
            }
            if (!engine.canExecute()) engine.setExecutable(true)

            val logFile = File(context.getExternalFilesDir(null), "$binaryName.log")

            // libonnxruntime.so is installed alongside the engine binary in filesDir
            // (via ModelImportActivity's runtime-file import), not under a system lib path.
            val libDir = context.filesDir.absolutePath

            // mksh -T- detaches the child from the controlling tty, reparenting it
            // to init so it survives shell exit. Equivalent to nohup + setsid.
            val args = engineArgs.joinToString(" ")
            val shellCmd = "LD_LIBRARY_PATH=$libDir ${engine.absolutePath} $args >> ${logFile.absolutePath} 2>&1 &"

            return@withContext try {
                val proc = ProcessBuilder("/system/bin/sh", "-T-", "-c", shellCmd)
                    .redirectErrorStream(true)
                    .start()
                proc.waitFor() // shell exits immediately; daemon lives on under init

                val pid = resolveEnginePid(binaryName)
                if (pid > 0) {
                    applyOomImmunity(pid)
                    Log.i(TAG, "Engine daemon PID=$pid log=${logFile.absolutePath}")
                    Result.success(DaemonHandle(pid, logFile.absolutePath))
                } else {
                    Result.failure(RuntimeException("Engine launched but PID resolution failed — check ${logFile.absolutePath}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun isRunning(): Boolean = resolveEnginePid(binaryName) > 0

    fun stop() {
        val pid = resolveEnginePid(binaryName)
        if (pid > 0) {
            try { Runtime.getRuntime().exec(arrayOf("kill", "-15", pid.toString())).waitFor() }
            catch (_: Exception) {}
            Log.i(TAG, "Sent SIGTERM to engine PID=$pid")
        }
    }

    private fun resolveEnginePid(binaryName: String): Int {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("pidof", binaryName))
            proc.inputStream.bufferedReader().readLine()
                ?.trim()?.split(" ")?.firstOrNull()?.toIntOrNull() ?: -1
        } catch (_: Exception) { -1 }
    }

    private fun applyOomImmunity(pid: Int) {
        // Root only. Silent no-op without root — caller does not need to handle failure.
        try {
            Runtime.getRuntime()
                .exec(arrayOf("su", "-c", "echo -1000 > /proc/$pid/oom_score_adj"))
                .waitFor()
            Log.i(TAG, "oom_score_adj set to -1000 for PID=$pid")
        } catch (_: Exception) {
            Log.d(TAG, "oom_score_adj: root not available; daemon runs at app priority")
        }
    }

    companion object {
        private const val TAG    = "DaemonLauncher"
        const val ENGINE_BINARY  = "ort_engine"
        const val ENGINE_PORT    = 8080
    }
}
