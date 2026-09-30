package com.example.a2uilibrarydemo.ai

import com.example.a2uilibrarydemo.a2ui.BASIC_CATALOG_V1_ID
import com.example.a2uilibrarydemo.a2ui.DEMO_SURFACE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** The Gemini model used to generate A2UI protocol scripts. Fast + cheap is plenty for this task. */
private const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

private const val ENDPOINT_TEMPLATE =
    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"

/**
 * The contract taught to Gemini so it emits a valid A2UI JSON-Lines script for the app's runtime.
 * Every rule here mirrors what [com.example.a2uilibrarydemo.a2ui.A2uiDemoPrompts] already had to
 * get right by hand — see that file's doc comments for the full story.
 */
private val SYSTEM_INSTRUCTION = """
    You are a UI-generating backend for an A2UI ("Agent-to-UI") client app. Given a user's
    request, you must respond with ONLY a JSON-Lines script (one JSON object per line, no markdown
    code fences, no commentary) describing a UI using the A2UI protocol v0.9 and the A2UI Basic
    Catalog V1 component set. The client will feed your output, line by line, directly into a live
    A2uiMessageProcessor.

    STRICT PROTOCOL RULES:
    1. Every line is a single JSON object. `"version":"v0.9"` MUST be a sibling key of the
       message-type key on EVERY line (createSurface, updateDataModel, updateComponents) — it is
       validated per-line, not once for the whole stream.
    2. The FIRST line must be:
       {"version":"v0.9","createSurface":{"surfaceId":"$DEMO_SURFACE_ID","catalogId":"$BASIC_CATALOG_V1_ID"}}
    3. If any component needs an interactively-writable value (CheckBox.value, TextField.value,
       Slider.value, ChoicePicker.value, DateTimeInput.value), you MUST:
       a. Emit an `updateDataModel` line seeding its initial value BEFORE the `updateComponents`
          line that references it, using an ABSOLUTE path (leading "/"), e.g.:
          {"version":"v0.9","updateDataModel":{"surfaceId":"$DEMO_SURFACE_ID","path":"/cb-1","value":false}}
       b. Set that component's own "value" property to a binding reference {"path":"cb-1"}
          (NOT a leading slash, and NOT a plain literal) — a plain literal silently disables the
          widget (renders, but taps/typing do nothing).
    4. The LAST line(s) must be one or more `updateComponents` messages:
       {"version":"v0.9","updateComponents":{"surfaceId":"$DEMO_SURFACE_ID","components":[ ... ]}}
       The `components` array is a flat list of component objects (not nested JSON) that reference
       each other by string id. The FIRST updateComponents message MUST include a component with
       id "root" — the renderer always looks for that id as the entry point of the surface.
    5. Button clicks use a separate mechanism, NOT data-binding:
       {"id":"btn-1","component":"Button","child":"btn-1-text","variant":"primary","action":{"event":{"name":"someEventName"}}}

    AVAILABLE COMPONENTS (A2UI Basic Catalog V1) and their key properties:
    - Text: {"component":"Text","text":"...","variant":"h1"|"h2"|"h3"|"h4"|"h5"|"caption"|"body"}
    - Image: {"component":"Image","url":"...","variant":"icon"|"avatar"|"mediumFeature","fit":"cover"|"contain"(optional),"description":"..."}
    - Icon: {"component":"Icon","name":"<material icon name, e.g. favorite, star, share, settings, home>"}
    - Video: {"component":"Video","url":"<direct mp4 url>"}
    - AudioPlayer: {"component":"AudioPlayer","url":"<direct mp3 url>","description":"..."}
    - Row: {"component":"Row","children":["id1","id2"],"justify":"start"|"end"|"center"|"spaceBetween"|"spaceEvenly"}
    - Column: {"component":"Column","children":["id1","id2"],"align":"start"|"end"|"center"|"stretch"}
    - List: {"component":"List","direction":"vertical"|"horizontal","align":"stretch","children":["id1","id2"]}
    - Card: {"component":"Card","child":"contentId"}
    - Tabs: {"component":"Tabs","tabs":[{"title":"First","child":"id1"},{"title":"Second","child":"id2"}]}
    - Modal: {"component":"Modal","trigger":"triggerButtonId","content":"contentId"}
    - Divider: {"component":"Divider","axis":"horizontal"|"vertical"}
    - Button: {"component":"Button","child":"textChildId","variant":"default"|"primary"|"borderless","action":{"event":{"name":"eventName"}}}
    - TextField: {"component":"TextField","label":"...","variant":"shortText"|"longText"|"number"|"obscured","value":{"path":"id"}}
    - CheckBox: {"component":"CheckBox","label":"...","value":{"path":"id"}}
    - ChoicePicker: {"component":"ChoicePicker","label":"...","variant":"mutuallyExclusive"|"multipleSelection","displayStyle":"checkbox"|"chips","options":[{"label":"A","value":"a"}],"value":{"path":"id"}}
      (mutuallyExclusive value is a single-element JSON array like ["a"]; multipleSelection value is a JSON array of selected values)
    - Slider: {"component":"Slider","label":"...","min":0,"max":100,"value":{"path":"id"}}
    - DateTimeInput: {"component":"DateTimeInput","label":"...","enableDate":true,"enableTime":false,"value":{"path":"id"}}

    Use only these components and properties. Prefer real, publicly reachable URLs for Image
    (e.g. https://picsum.photos/seed/<anything>/<width>/<height>), Video
    (https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4), and
    AudioPlayer (https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3) unless the user
    asks for something else specific. Keep the generated UI reasonably small and directly relevant
    to the user's request. Output nothing but the JSON-Lines script.
""".trimIndent()

class GeminiApiException(message: String) : Exception(message)

/**
 * Calls the Gemini API's `generateContent` REST endpoint (no SDK dependency — plain
 * [HttpURLConnection] + `org.json`) with [SYSTEM_INSTRUCTION] as the system prompt and [userPrompt]
 * as the user turn, and returns the model's raw text response split into non-blank lines (each
 * expected to be one A2UI protocol JSON-Lines message).
 */
suspend fun generateA2uiScript(
    apiKey: String,
    userPrompt: String,
    model: String = DEFAULT_MODEL,
): List<String> = withContext(Dispatchers.IO) {
    if (apiKey.isBlank()) {
        throw GeminiApiException("No Gemini API key set. Add one in Settings.")
    }

    val requestBody = JSONObject().apply {
        put(
            "systemInstruction",
            JSONObject().put(
                "parts",
                JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION)),
            ),
        )
        put(
            "contents",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userPrompt))),
            ),
        )
        put(
            "generationConfig",
            JSONObject().put("responseMimeType", "text/plain").put("temperature", 0.4),
        )
    }

    val url = URL(String.format(ENDPOINT_TEMPLATE, model))
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 30_000
        readTimeout = 60_000
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("x-goog-api-key", apiKey)
    }

    try {
        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(requestBody.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()

        if (responseCode !in 200..299) {
            val message = runCatching {
                JSONObject(responseText).getJSONObject("error").getString("message")
            }.getOrDefault(responseText.ifBlank { "HTTP $responseCode" })
            throw GeminiApiException("Gemini API error ($responseCode): $message")
        }

        val json = JSONObject(responseText)
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason")
            throw GeminiApiException(
                "Gemini returned no candidates" +
                    (blockReason?.takeIf { it.isNotBlank() }?.let { " (blockReason=$it)" } ?: ""),
            )
        }

        val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        val text = buildString {
            for (i in 0 until parts.length()) {
                append(parts.getJSONObject(i).optString("text"))
            }
        }

        stripMarkdownFences(text)
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    } finally {
        connection.disconnect()
    }
}

/** Strips a leading/trailing ```json or ``` fence in case the model wraps its output anyway. */
private fun stripMarkdownFences(text: String): String {
    var result = text.trim()
    if (result.startsWith("```")) {
        result = result.substringAfter("\n")
    }
    if (result.endsWith("```")) {
        result = result.substringBeforeLast("```")
    }
    return result
}
