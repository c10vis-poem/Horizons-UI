package com.horizons.core.agent

import android.content.Context
import android.util.Log
import com.horizons.core.agent.tools.*
import com.horizons.core.llm.LlmRuntime
import com.horizons.core.perf.GameModeBoost.gameBoosted
import com.horizons.core.shell.TaskerBridge
import com.horizons.core.state.AppStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Agentic inference loop.
 *
 * Each call to [run] drives a full ReAct cycle:
 *   1. Build prompt = system + history + user message
 *   2. Stream LLM output, scan for <tool>…</tool> tags via AgentToolParser
 *   3. Execute the tool, inject <result>…</result> into context
 *   4. Re-invoke LLM with updated context
 *   5. Repeat until the LLM emits <tool>{"name":"done"}</tool> or [MAX_TURNS] is reached
 *
 * Emitted values to the caller: text chunks (display) and status updates prefixed with "§".
 * The caller appends text chunks to the visible reply; "§" lines are status/debug only.
 */
class AgentLoop(
    private val context: Context,
    private val llmProvider: () -> LlmRuntime,
    private val tasker: TaskerBridge,
    private val stateStore: AppStateStore,
) {
    private val appTool     = AppTool(context)
    private val alarmTool   = AlarmTool(context)
    private val calTool     = CalendarTool(context)
    private val contactTool = ContactsTool(context)
    private val deviceTool  = DeviceTool(context)
    private val mediaTool   = MediaTool(context)
    private val notifTool   = NotificationTool(context)
    private val clipTool    = ClipboardTool(context)
    private val sysTool     = SystemInfoTool(context)

    fun run(userMessage: String): Flow<String> = flow {
        val context = StringBuilder()
        context.append(AgentSystemPrompt.SYSTEM)
        context.append("\n\nUser: $userMessage\nAssistant:")

        val parser = AgentToolParser()
        var turns  = 0

        while (turns < MAX_TURNS) {
            turns++
            var pendingTool: AgentTool? = null

            // Stream one LLM turn
            llmProvider().stream(context.toString()).gameBoosted(this@AgentLoop.context).collect { chunk ->
                val result = parser.feed(chunk)
                if (result.displayText.isNotEmpty()) emit(result.displayText)
                if (result.tool != null) pendingTool = result.tool
            }

            val tool = pendingTool
            if (tool == null || tool is AgentTool.Done) break

            // Execute tool
            emit("\n")  // visual newline before status
            val toolResult = executeTool(tool)
            Log.d(TAG, "Tool ${tool::class.simpleName}: ok=${toolResult.ok} data=${toolResult.data.take(120)}")

            // Inject result into context for next turn
            val toolJson  = toolToJson(tool)
            val resultTag = AgentSystemPrompt.wrapResult(toolResult)
            context.append("\n<tool>$toolJson</tool>\n$resultTag\n")

            if (!toolResult.ok) {
                // Surface error to user and let LLM decide how to recover
                context.append("The tool failed. Explain the issue to the user and suggest next steps.\nAssistant:")
            } else {
                context.append("Continue based on the result above.\nAssistant:")
            }

            parser.reset()
        }

        if (turns >= MAX_TURNS) emit("\n[Agent reached max tool turns ($MAX_TURNS)]")
    }

    private suspend fun executeTool(tool: AgentTool): ToolResult = when (tool) {
        is AgentTool.LaunchApp          -> appTool.launch(tool.app)
        is AgentTool.ListApps           -> appTool.list()
        is AgentTool.SetAlarm           -> alarmTool.setAlarm(tool.time, tool.label, tool.days)
        is AgentTool.SetTimer           -> alarmTool.setTimer(tool.duration_seconds, tool.label)
        is AgentTool.ReadCalendar       -> calTool.readUpcoming(tool.lookahead_days)
        is AgentTool.CreateCalendarEvent -> calTool.createEvent(tool.title, tool.start, tool.end, tool.description, tool.location)
        is AgentTool.SearchContacts     -> contactTool.search(tool.query)
        is AgentTool.WifiControl        -> deviceTool.wifi(tool.action)
        is AgentTool.BluetoothControl   -> deviceTool.bluetooth(tool.action)
        is AgentTool.VolumeControl      -> deviceTool.volume(tool.stream, tool.level)
        is AgentTool.BrightnessControl  -> deviceTool.brightness(tool.level)
        is AgentTool.DndControl         -> deviceTool.dnd(tool.mode)
        is AgentTool.Flashlight         -> deviceTool.flashlight(tool.action)
        is AgentTool.MediaControl       -> mediaTool.control(tool.action)
        is AgentTool.ReadNotifications  -> notifTool.read(AgentNotificationListener.active.value)
        is AgentTool.PostNotification   -> notifTool.post(tool.title, tool.body, tool.channel)
        is AgentTool.ReadClipboard      -> clipTool.read()
        is AgentTool.WriteClipboard     -> clipTool.write(tool.text)
        is AgentTool.HttpFetch          -> runHttpFetch(tool)
        is AgentTool.WebSearch          -> runWebSearch(tool)
        is AgentTool.BatteryStatus      -> sysTool.battery()
        is AgentTool.NetworkStatus      -> sysTool.network()
        is AgentTool.StorageStatus      -> sysTool.storage()
        is AgentTool.TaskerTask         -> runTasker(tool)
        is AgentTool.Done               -> ToolResult("done", true, "")
    }

    private suspend fun runHttpFetch(tool: AgentTool.HttpFetch): ToolResult = withContext(Dispatchers.IO) {
        try {
            val conn = URL(tool.url).openConnection() as HttpURLConnection
            conn.requestMethod = tool.method.uppercase()
            conn.connectTimeout = 15_000
            conn.readTimeout   = 30_000
            conn.setRequestProperty("Content-Type", tool.content_type)
            if (tool.bearer_token_key.isNotBlank()) {
                val token = stateStore.get(tool.bearer_token_key)
                if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
            }
            if (tool.body.isNotEmpty()) {
                conn.doOutput = true
                conn.outputStream.use { it.write(tool.body.toByteArray()) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.readText() ?: ""
            ToolResult("http_fetch", code in 200..299, body.take(8192))
        } catch (e: Exception) {
            ToolResult("http_fetch", false, e.message ?: "network error")
        }
    }

    private suspend fun runWebSearch(tool: AgentTool.WebSearch): ToolResult = withContext(Dispatchers.IO) {
        try {
            val q = java.net.URLEncoder.encode(tool.query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$q&format=json&no_html=1&skip_disambig=1"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            val code = conn.responseCode
            if (code !in 200..299) return@withContext ToolResult("web_search", false, "HTTP $code")
            val raw = conn.inputStream.bufferedReader().readText()
            val root = org.json.JSONObject(raw)
            val out = org.json.JSONArray()
            root.optString("AbstractText").takeIf { it.isNotBlank() }?.let {
                out.put(org.json.JSONObject().put("title", root.optString("Heading"))
                    .put("snippet", it).put("url", root.optString("AbstractURL")))
            }
            val related = root.optJSONArray("RelatedTopics") ?: org.json.JSONArray()
            var i = 0
            while (i < related.length() && out.length() < tool.max_results) {
                val item = related.optJSONObject(i)
                if (item != null && item.has("Text")) {
                    out.put(org.json.JSONObject().put("snippet", item.optString("Text"))
                        .put("url", item.optString("FirstURL")))
                }
                i++
            }
            ToolResult("web_search", true, out.toString())
        } catch (e: Exception) {
            ToolResult("web_search", false, e.message ?: "search error")
        }
    }

    private fun runTasker(tool: AgentTool.TaskerTask): ToolResult {
        val params = buildList {
            if (tool.param1.isNotBlank()) add("par1" to tool.param1)
            if (tool.param2.isNotBlank()) add("par2" to tool.param2)
        }.toTypedArray()
        val result = tasker.runTask(tool.task_name, *params)
        return if (result.isSuccess)
            ToolResult("tasker_task", true, "Task '${tool.task_name}' triggered")
        else
            ToolResult("tasker_task", false, result.exceptionOrNull()?.message ?: "Tasker unavailable")
    }

    /** Serialize an AgentTool back to the JSON string the LLM would have emitted. */
    private fun toolToJson(tool: AgentTool): String {
        val obj = org.json.JSONObject()
        when (tool) {
            is AgentTool.LaunchApp           -> { obj.put("name","launch_app");   obj.put("args", org.json.JSONObject().put("app", tool.app)) }
            is AgentTool.ListApps            -> obj.put("name","list_apps")
            is AgentTool.SetAlarm            -> { obj.put("name","set_alarm");    obj.put("args", org.json.JSONObject().put("time",tool.time).put("label",tool.label)) }
            is AgentTool.SetTimer            -> { obj.put("name","set_timer");    obj.put("args", org.json.JSONObject().put("duration_seconds",tool.duration_seconds)) }
            is AgentTool.ReadCalendar        -> { obj.put("name","read_calendar"); obj.put("args",org.json.JSONObject().put("lookahead_days",tool.lookahead_days)) }
            is AgentTool.CreateCalendarEvent -> { obj.put("name","create_event"); obj.put("args",org.json.JSONObject().put("title",tool.title).put("start",tool.start).put("end",tool.end)) }
            is AgentTool.SearchContacts      -> { obj.put("name","search_contacts"); obj.put("args",org.json.JSONObject().put("query",tool.query)) }
            is AgentTool.WifiControl         -> { obj.put("name","wifi");         obj.put("args",org.json.JSONObject().put("action",tool.action)) }
            is AgentTool.BluetoothControl    -> { obj.put("name","bluetooth");    obj.put("args",org.json.JSONObject().put("action",tool.action)) }
            is AgentTool.VolumeControl       -> { obj.put("name","volume");       obj.put("args",org.json.JSONObject().put("stream",tool.stream).put("level",tool.level)) }
            is AgentTool.BrightnessControl   -> { obj.put("name","brightness");   obj.put("args",org.json.JSONObject().put("level",tool.level)) }
            is AgentTool.DndControl          -> { obj.put("name","dnd");          obj.put("args",org.json.JSONObject().put("mode",tool.mode)) }
            is AgentTool.Flashlight          -> { obj.put("name","flashlight");   obj.put("args",org.json.JSONObject().put("action",tool.action)) }
            is AgentTool.MediaControl        -> { obj.put("name","media");        obj.put("args",org.json.JSONObject().put("action",tool.action)) }
            is AgentTool.ReadNotifications   -> obj.put("name","read_notifications")
            is AgentTool.PostNotification    -> { obj.put("name","post_notification"); obj.put("args",org.json.JSONObject().put("title",tool.title).put("body",tool.body)) }
            is AgentTool.ReadClipboard       -> obj.put("name","read_clipboard")
            is AgentTool.WriteClipboard      -> { obj.put("name","write_clipboard"); obj.put("args",org.json.JSONObject().put("text",tool.text)) }
            is AgentTool.HttpFetch           -> { obj.put("name","http_fetch");   obj.put("args",org.json.JSONObject().put("url",tool.url).put("method",tool.method).put("body",tool.body)) }
            is AgentTool.WebSearch           -> { obj.put("name","web_search");   obj.put("args",org.json.JSONObject().put("query",tool.query).put("max_results",tool.max_results)) }
            is AgentTool.BatteryStatus       -> obj.put("name","battery")
            is AgentTool.NetworkStatus       -> obj.put("name","network")
            is AgentTool.StorageStatus       -> obj.put("name","storage")
            is AgentTool.TaskerTask          -> { obj.put("name","tasker_task");  obj.put("args",org.json.JSONObject().put("task_name",tool.task_name)) }
            is AgentTool.Done                -> obj.put("name","done")
        }
        return obj.toString()
    }

    companion object {
        private const val TAG = "AgentLoop"
        private const val MAX_TURNS = 12
    }
}
