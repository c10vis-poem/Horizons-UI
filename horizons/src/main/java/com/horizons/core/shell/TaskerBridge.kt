package com.horizons.core.shell

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

// Requires user setting: Tasker → Settings → Misc → Allow External Access
class TaskerBridge(private val context: Context) {

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

    fun runTask(taskName: String, vararg params: Pair<String, String>): Result<Unit> {
        if (!isTaskerInstalled()) {
            return Result.failure(IllegalStateException("Tasker not installed"))
        }
        return try {
            val intent = Intent(ACTION_TASK).apply {
                setPackage(TASKER_PACKAGE)
                // version_number=1.1 is required — modern Tasker silently ignores
                // ACTION_TASK broadcasts that omit it.
                putExtra("version_number", "1.1")
                putExtra("task_name", taskName)
                putExtra("task_priority", 5)
                params.forEachIndexed { i, p -> putExtra("par${i + 1}", p.second) }
            }
            context.sendBroadcast(intent)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun runScene(sceneName: String): Result<Unit> {
        if (!isTaskerInstalled()) {
            return Result.failure(IllegalStateException("Tasker not installed"))
        }
        return try {
            val intent = Intent(ACTION_TASK).apply {
                setPackage(TASKER_PACKAGE)
                putExtra("version_number", "1.1")
                putExtra("scene_name", sceneName)
            }
            context.sendBroadcast(intent)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * USER-INITIATED ONLY. The sole caller is the Terminal room, where the user types
     * the command themselves. This is deliberately NOT reachable from [AgentLoop]:
     * this UID holds MANAGE_EXTERNAL_STORAGE, so anything run here can delete
     * anything under /storage/emulated/0. Never wire this to a model-generated string.
     */
    suspend fun runInteractiveCommand(
        command: String,
        @Suppress("UNUSED_PARAMETER") workdir: String = "",
    ): ShellResult {
        // Run as the Horizons process directly. This UID holds MANAGE_EXTERNAL_STORAGE,
        // so it reaches all of /storage/emulated/0 without involving Termux at all.
        // We deliberately do NOT route through Termux — its RUN_COMMAND service crashes
        // when external access isn't granted, and a crashed Termux never returns a
        // result, hanging the call forever. The in-app terminal is self-contained.
        return runDirectShell(command)
    }

    private suspend fun runDirectShell(command: String): ShellResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val proc = ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(false)
                    .start()
                val stdout = proc.inputStream.bufferedReader().readText()
                val stderr = proc.errorStream.bufferedReader().readText()
                val exit = proc.waitFor()
                ShellResult(exit, stdout.trimEnd(), stderr.trimEnd())
            } catch (e: Exception) {
                ShellResult(1, "", "Direct shell error: ${e.message}")
            }
        }

    fun isTaskerInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(TASKER_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun isTermuxInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val TASKER_PACKAGE = "net.dinglisch.android.taskerm"
        const val TERMUX_PACKAGE = "com.termux"
        const val ACTION_TASK = "net.dinglisch.android.tasker.ACTION_TASK"
    }
}
