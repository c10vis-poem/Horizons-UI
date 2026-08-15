package com.horizons.core.agent

import org.json.JSONObject

/**
 * Scans streaming LLM output for <tool>…</tool> tags and deserializes them into AgentTool.
 * Handles the tag being split across multiple stream chunks.
 */
class AgentToolParser {

    private val buf = StringBuilder()

    /**
     * Feed the next chunk from the LLM stream. Returns a parsed AgentTool if a complete
     * <tool>…</tool> block was closed in this chunk, null otherwise.
     * Also returns the "display" text — everything before the <tool> tag — for streaming to UI.
     */
    fun feed(chunk: String): ParseResult {
        buf.append(chunk)
        val full = buf.toString()

        val start = full.indexOf("<tool>")
        val end   = full.indexOf("</tool>")

        if (start == -1) {
            // No tool tag started yet — all text is display text
            val text = full
            buf.clear()
            return ParseResult(displayText = text, tool = null)
        }

        if (end == -1) {
            // Tag opened but not closed — display text up to the tag; keep buffer
            val text = full.substring(0, start)
            return ParseResult(displayText = text, tool = null)
        }

        // Full tag present
        val beforeTag  = full.substring(0, start)
        val tagContent = full.substring(start + 6, end).trim()
        val afterTag   = full.substring(end + 7)

        buf.clear()
        buf.append(afterTag)  // keep any text after the closing tag for next round

        val tool = runCatching { parseTool(tagContent) }.getOrNull()
        return ParseResult(displayText = beforeTag, tool = tool)
    }

    fun reset() { buf.clear() }

    private fun parseTool(json: String): AgentTool {
        val obj  = JSONObject(json)
        val name = obj.getString("name")
        val args = obj.optJSONObject("args") ?: JSONObject()

        return when (name) {
            "launch_app"         -> AgentTool.LaunchApp(args.getString("app"))
            "list_apps"          -> AgentTool.ListApps
            "set_alarm"          -> AgentTool.SetAlarm(
                time  = args.getString("time"),
                label = args.optString("label", ""),
                days  = args.optJSONArray("days")?.let { a ->
                    (0 until a.length()).map { a.getString(it) }
                } ?: emptyList(),
            )
            "set_timer"          -> AgentTool.SetTimer(
                duration_seconds = args.getInt("duration_seconds"),
                label = args.optString("label", ""),
            )
            "read_calendar"      -> AgentTool.ReadCalendar(args.optInt("lookahead_days", 7))
            "create_event"       -> AgentTool.CreateCalendarEvent(
                title       = args.getString("title"),
                start       = args.getString("start"),
                end         = args.getString("end"),
                description = args.optString("description", ""),
                location    = args.optString("location", ""),
            )
            "search_contacts"    -> AgentTool.SearchContacts(args.getString("query"))
            "wifi"               -> AgentTool.WifiControl(args.getString("action"))
            "bluetooth"          -> AgentTool.BluetoothControl(args.getString("action"))
            "volume"             -> AgentTool.VolumeControl(
                stream = args.getString("stream"),
                level  = args.optInt("level", -1),
            )
            "brightness"         -> AgentTool.BrightnessControl(args.getInt("level"))
            "dnd"                -> AgentTool.DndControl(args.getString("mode"))
            "flashlight"         -> AgentTool.Flashlight(args.getString("action"))
            "media"              -> AgentTool.MediaControl(args.getString("action"))
            "read_notifications" -> AgentTool.ReadNotifications
            "post_notification"  -> AgentTool.PostNotification(
                title   = args.getString("title"),
                body    = args.getString("body"),
                channel = args.optString("channel", "agent"),
            )
            "read_clipboard"     -> AgentTool.ReadClipboard
            "write_clipboard"    -> AgentTool.WriteClipboard(args.getString("text"))
            // Removed: the agent had unrestricted `sh -c` under MANAGE_EXTERNAL_STORAGE,
            // reaching all of /storage/emulated/0 with no allowlist, path guard, or
            // confirmation. Shell is now user-initiated only, via the Terminal room.
            // Rejected explicitly (not via the generic `else`) so re-adding it is deliberate.
            "shell"              -> throw IllegalArgumentException(
                "shell is not an agent tool; run commands yourself in the Terminal room"
            )
            "battery"            -> AgentTool.BatteryStatus
            "network"            -> AgentTool.NetworkStatus
            "storage"            -> AgentTool.StorageStatus
            "tasker_task"        -> AgentTool.TaskerTask(
                task_name = args.getString("task_name"),
                param1    = args.optString("param1", ""),
                param2    = args.optString("param2", ""),
            )
            "done"               -> AgentTool.Done
            else -> throw IllegalArgumentException("Unknown tool: $name")
        }
    }

    data class ParseResult(val displayText: String, val tool: AgentTool?)
}
