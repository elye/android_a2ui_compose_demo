package com.example.a2uilibrarydemo.a2ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.material3.a2ui.A2uiSurfaceDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import kotlinx.coroutines.flow.collect
import org.json.JSONObject

private const val TAG = "A2uiDemoScreen"

/**
 * Renders a single [DemoPrompt] by feeding its A2UI JSON-Lines script into a fresh
 * [androidx.a2ui.model.processor.A2uiMessageProcessor] and displaying the resulting surface via
 * [A2uiSurface].
 *
 * A brand new processor/parser pair is created per [prompt] (keyed by [DemoPrompt.id]) so that
 * switching prompts always starts from a clean slate.
 */
@Composable
fun A2uiDemoScreen(prompt: DemoPrompt, modifier: Modifier = Modifier) {
    A2uiMessagesRenderer(
        messages = prompt.messages,
        key = prompt.id,
        modifier = modifier,
        loadingLabel = "Loading ${prompt.title}…",
    )
}

/**
 * Reusable core of the demo: feeds an arbitrary A2UI JSON-Lines [messages] script into a fresh
 * [androidx.a2ui.model.processor.A2uiMessageProcessor] and renders the resulting surface via
 * [A2uiSurface]. Used both by [A2uiDemoScreen] (hand-written catalog scripts) and the AI Prompt
 * screen (scripts generated on the fly by Gemini).
 *
 * A brand new processor/parser pair is created whenever [key] changes, so switching to a new
 * script always starts from a clean slate.
 */
@Composable
fun A2uiMessagesRenderer(
    messages: List<String>,
    key: Any,
    modifier: Modifier = Modifier,
    loadingLabel: String = "Loading…",
) {
    val processor = remember(key) {
        A2uiMessageProcessor(listOf(buildDemoCatalog()))
    }
    val parser = remember(key) { A2uiMessageParser() }

    // The processor only enqueues incoming messages onto an internal channel; collectMessages()
    // must be actively running to drain that queue and actually apply the updates.
    LaunchedEffect(processor) {
        processor.collectMessages()
    }

    // Surface any silently-caught protocol/validation errors so we can diagnose issues on device
    // via `adb logcat -s A2uiDemoScreen`.
    LaunchedEffect(processor) {
        processor.outboundEvents.collect { event ->
            if (event is A2uiClientErrorMessage) {
                Log.e(
                    TAG,
                    "A2UI client error: code=${event.code} surfaceId=${event.surfaceId} " +
                        "message=${event.message} context=${event.context}",
                )
            } else {
                Log.d(TAG, "A2UI outbound event: $event")
            }
        }
    }

    // Feed every line of the script once the processor/parser for this key are ready.
    LaunchedEffect(key, processor, parser) {
        for (line in messages) {
            Log.d(TAG, "Sending line: $line")
            processor.processInput(parser, line)
        }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()
    val surface = activeSurfaces.firstOrNull()
    // A List component renders its own LazyColumn internally; nesting that inside a
    // verticalScroll()'d container throws "measured with an infinity maximum height constraints".
    // Everything else (Text, Column, Card, Tabs, etc.) doesn't self-scroll, so a long generated
    // script would otherwise be cut off with no way to see the rest — only skip the outer scroll
    // when the script contains a List.
    val hasSelfScrollingList = remember(key) { messages.any { it.containsListComponent() } }

    Box(modifier = modifier.fillMaxSize()) {
        if (surface != null) {
            A2uiSurface(
                surfaceModel = surface,
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (hasSelfScrollingList) it else it.verticalScroll(rememberScrollState()) }
                    .padding(16.dp),
                loadingContent = { A2uiSurfaceDefaults.LoadingIndicator() },
                errorContent = { exception -> A2uiSurfaceDefaults.ErrorFallback(exception) },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Text(
                    text = loadingLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Whether this single A2UI JSON-Lines message declares a `"component":"List"`. */
private fun String.containsListComponent(): Boolean = runCatching {
    val components = JSONObject(this).optJSONObject("updateComponents")
        ?.optJSONArray("components")
        ?: return@runCatching false
    (0 until components.length()).any { i -> components.getJSONObject(i).optString("component") == "List" }
}.getOrDefault(false)
