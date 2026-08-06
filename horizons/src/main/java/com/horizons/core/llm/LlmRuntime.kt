package com.horizons.core.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface LlmRuntime {
    fun stream(prompt: String): Flow<String>

    fun streamAudio(wav: ByteArray, prompt: String = ""): Flow<String> =
        stream(if (prompt.isNotBlank()) prompt else "[audio input not supported by this runtime]")

    fun streamImage(jpeg: ByteArray, prompt: String = ""): Flow<String> =
        stream(if (prompt.isNotBlank()) "[image] $prompt" else "[image input not supported by this runtime]")

    /**
     * Human-readable backend identity for the Models / Chat panes.
     *
     * Consumers match on SUBSTRINGS, not a prefix: `contains("no backend")`,
     * `contains("Cloud")`, and `contains("Hexagon") || contains("NPU")` — see
     * ChatPane, RouterPane and MonitorPane. Keep those tokens intact when
     * changing a status string.
     *
     * This comment previously claimed the UI treated `startsWith("Adreno 830")`
     * as "ready". No code has ever done that in this tree, and `HomeGrid.kt`
     * does not read `backendStatus` at all — but the claim was copied into
     * CLAUDE.md, AGENT-BRIEF and CRASH-ANALYSIS as a live bug in a frozen file,
     * where it sat unfixable-by-decree for several sessions. It was a phantom.
     * Corrected here so it stops being re-derived.
     */
    val backendStatus: StateFlow<String>
        get() = idleStatus

    /** Timing from the most recently completed stream() call. Null until the first response. */
    val perfMetrics: StateFlow<PerfMetrics?>
        get() = noMetrics

    /** No-op default — runtimes that need warm-up (daemon startup) override. */
    fun preWarm() {}

    data class PerfMetrics(
        val firstTokenMs: Long,
        val tokensPerSec: Double,
        val tokenCount: Int,
    )

    companion object {
        private val idleStatus: StateFlow<String> = MutableStateFlow("idle")
        private val noMetrics: StateFlow<PerfMetrics?> = MutableStateFlow(null)
    }
}
