package com.example.a2uilibrarydemo.a2ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
    val processor = remember(prompt.id) {
        A2uiMessageProcessor(listOf(buildDemoCatalog()))
    }
    val parser = remember(prompt.id) { A2uiMessageParser() }

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

    // Feed every line of the demo script once the processor/parser for this prompt are ready.
    LaunchedEffect(prompt.id, processor, parser) {
        for (line in prompt.messages) {
            Log.d(TAG, "Sending line: $line")
            processor.processInput(parser, line)
        }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()
    val surface = activeSurfaces.firstOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        if (surface != null) {
            // Deliberately no outer Modifier.verticalScroll() here: some A2UI Basic Catalog V1
            // components (e.g. List) render their own LazyColumn internally, and nesting a
            // LazyColumn inside a verticalScroll()'d container throws
            // "Vertically scrollable component was measured with an infinity maximum height
            // constraints". Each rendered component tree is responsible for its own scrolling.
            A2uiSurface(
                surfaceModel = surface,
                modifier = Modifier
                    .fillMaxSize()
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
                    text = "Loading ${prompt.title}…",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
