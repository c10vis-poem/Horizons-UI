package com.horizons.core.agent

/**
 * Every capability the agent can invoke. Each subclass maps 1:1 to a JSON tool-call
 * the LLM emits inside <tool>…</tool> tags. The AgentLoop parses the tag, deserializes
 * into the matching subclass, and routes to the corresponding ToolHandler.
 *
 * Design rule: params are plain primitives / strings. No Android types cross this boundary.
 * The tool handlers own all context lookups (ContentResolver, SystemService, etc.).
 */
sealed class AgentTool {

    // ── Apps ──────────────────────────────────────────────────────────────────
    /** Launch any installed app by its human-readable label or package name. */
    data class LaunchApp(val app: String) : AgentTool()

    /** List installed launchable apps; returns JSON array of {label, package}. */
    object ListApps : AgentTool()

    // ── Alarms / Timers ───────────────────────────────────────────────────────
    /** Set an alarm. time = "HH:MM" 24-hour, days = ["Mon","Tue",...] optional. */
    data class SetAlarm(val time: String, val label: String = "", val days: List<String> = emptyList()) : AgentTool()

    /** Set a countdown timer. duration_seconds must be > 0. */
    data class SetTimer(val duration_seconds: Int, val label: String = "") : AgentTool()

    // ── Calendar ──────────────────────────────────────────────────────────────
    /** Read upcoming calendar events. lookahead_days default 7. */
    data class ReadCalendar(val lookahead_days: Int = 7) : AgentTool()

    /** Create a calendar event. start/end = ISO-8601 datetime strings. */
    data class CreateCalendarEvent(
        val title: String,
        val start: String,
        val end: String,
        val description: String = "",
        val location: String = "",
    ) : AgentTool()

    // ── Contacts ──────────────────────────────────────────────────────────────
    /** Search contacts by name. Returns JSON array of {name, phone, email}. */
    data class SearchContacts(val query: String) : AgentTool()

    // ── Device settings ───────────────────────────────────────────────────────
    /** Toggle WiFi on/off, or "status" to query current state. */
    data class WifiControl(val action: String) : AgentTool()  // "on" | "off" | "status"

    /** Toggle Bluetooth, or query status. */
    data class BluetoothControl(val action: String) : AgentTool()  // "on" | "off" | "status"

    /** Set or query volume. stream = "music"|"ring"|"alarm"|"notification". level 0–15. */
    data class VolumeControl(val stream: String, val level: Int = -1) : AgentTool()

    /** Set screen brightness 0–255, or -1 to query. */
    data class BrightnessControl(val level: Int) : AgentTool()

    /** Toggle Do Not Disturb. mode = "on"|"off"|"status". */
    data class DndControl(val mode: String) : AgentTool()

    /** Toggle flashlight. action = "on"|"off". */
    data class Flashlight(val action: String) : AgentTool()

    // ── Media ─────────────────────────────────────────────────────────────────
    /** Control media playback. action = "play"|"pause"|"next"|"prev"|"status". */
    data class MediaControl(val action: String) : AgentTool()

    // ── Notifications ─────────────────────────────────────────────────────────
    /** Read active notifications (requires NotificationListenerService enabled). */
    object ReadNotifications : AgentTool()

    /** Post a notification from the agent. */
    data class PostNotification(val title: String, val body: String, val channel: String = "agent") : AgentTool()

    // ── Clipboard ─────────────────────────────────────────────────────────────
    /** Read clipboard contents. */
    object ReadClipboard : AgentTool()

    /** Write text to clipboard. */
    data class WriteClipboard(val text: String) : AgentTool()

    // ── Cloud / HTTP ──────────────────────────────────────────────────────────
    /** HTTP request to any URL. Use for cloud APIs, SambaNova, OpenRouter, etc. */
    data class HttpFetch(
        val url: String,
        val method: String = "GET",
        val body: String = "",
        val content_type: String = "application/json",
        val bearer_token_key: String = "",  // AppStateStore key for the token, e.g. "api.sambanova"
    ) : AgentTool()

    /** Web search via DuckDuckGo Instant Answer API. Returns JSON results. */
    data class WebSearch(val query: String, val max_results: Int = 5) : AgentTool()

    // ── System info ───────────────────────────────────────────────────────────
    /** Get battery level, charging state, temperature. */
    object BatteryStatus : AgentTool()

    /** Get network info: type, IP, connected SSID. */
    object NetworkStatus : AgentTool()

    /** Get storage info: free/total for internal storage. */
    object StorageStatus : AgentTool()

    // ── Tasker bridge (last resort — prefer native tools above) ───────────────
    /** Trigger a named Tasker task. Prefer native tools; only use if no Android API exists. */
    data class TaskerTask(val task_name: String, val param1: String = "", val param2: String = "") : AgentTool()

    // ── Meta ──────────────────────────────────────────────────────────────────
    /** Agent signals it is done and the answer is in the preceding text. No further tool calls. */
    object Done : AgentTool()
}

/** Result returned to the LLM after a tool executes. */
data class ToolResult(
    val tool: String,
    val ok: Boolean,
    val data: String,  // JSON string or plain text
)
