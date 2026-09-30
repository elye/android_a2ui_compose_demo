package com.example.a2uilibrarydemo.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import com.example.a2uilibrarydemo.a2ui.A2uiMessagesRenderer
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Starter prompts for flows worth stepping through, e.g. wizards with a "Next"/"Submit" button. */
private val SUGGESTED_FLOW_PROMPTS = listOf(
    "Signup wizard" to "A 3-step signup wizard. Start by showing only step 1: name and email text fields and a 'Next' button.",
    "Support ticket" to "A support ticket form: a category choice picker, a description text field, and a 'Submit' button.",
    "Quiz" to "A one-question multiple choice quiz with 4 options and a 'Submit answer' button.",
    "Checkout" to "A checkout flow. Start by showing only a shipping address text field and a 'Continue to payment' button.",
)

/**
 * The "Multi-Step AI Demo" screen: unlike [AiPromptScreen] (one prompt -> one rendered UI), this
 * keeps a running Gemini conversation going. Each turn either comes from the user typing a new
 * instruction, or automatically from tapping a `Button` in the currently rendered A2UI surface
 * (captured via [A2uiMessagesRenderer]'s `onClientEvent`) — Gemini sees a description of what was
 * tapped plus the current data model, and responds with the next step's UI, continuing the flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConversationScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var promptText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var generatedMessages by remember { mutableStateOf<List<String>?>(null) }
    // Bumped on every turn so A2uiMessagesRenderer always starts from a clean processor/parser.
    var generationId by remember { mutableIntStateOf(0) }
    var stepCount by remember { mutableIntStateOf(0) }
    // Not Compose state: mutated directly and only ever read from within sendTurn's coroutine.
    val history = remember { mutableListOf<GeminiTurn>() }

    fun sendTurn(userText: String) {
        if (isLoading || userText.isBlank()) return
        val apiKey = GeminiApiKeyStore.getApiKey(context)
        if (apiKey.isBlank()) {
            errorMessage = "No Gemini API key set. Tap the settings icon to add one."
            return
        }

        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            runCatching { generateA2uiScript(apiKey = apiKey, userPrompt = userText, history = history) }
                .onSuccess { result ->
                    history.add(GeminiTurn(role = "user", text = userText))
                    history.add(GeminiTurn(role = "model", text = result.rawResponseText))
                    generatedMessages = result.messages
                    generationId += 1
                    stepCount += 1
                    promptText = ""
                }
                .onFailure { throwable ->
                    errorMessage = throwable.message ?: "Unknown error generating UI."
                }
            isLoading = false
        }
    }

    fun resetConversation() {
        history.clear()
        generatedMessages = null
        generationId += 1
        stepCount = 0
        errorMessage = null
        promptText = ""
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (stepCount == 0) "Start a multi-step flow" else "Step $stepCount",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row {
                    if (stepCount > 0) {
                        TextButton(onClick = { resetConversation() }) { Text("Reset") }
                    }
                    TextButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Text(" API key")
                    }
                }
            }

            Text(
                text = "Describe a starting UI, then tap buttons in the generated UI (or type " +
                    "another instruction below) to keep the conversation going.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(if (stepCount == 0) "Starting prompt" else "Next instruction (optional)") },
                placeholder = { Text("e.g. A 3-step signup wizard starting with name + email") },
                minLines = 2,
            )

            if (stepCount == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SUGGESTED_FLOW_PROMPTS.forEach { (label, suggestion) ->
                        OutlinedButton(onClick = { promptText = suggestion }) {
                            Text(label)
                        }
                    }
                }
            }

            Button(
                onClick = { sendTurn(promptText) },
                enabled = !isLoading && promptText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(
                    when {
                        isLoading -> "Generating…"
                        stepCount == 0 -> "Start"
                        else -> "Send"
                    },
                )
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        when {
            isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                CircularProgressIndicator()
            }

            generatedMessages != null -> A2uiMessagesRenderer(
                messages = generatedMessages!!,
                key = generationId,
                modifier = Modifier.fillMaxSize(),
                loadingLabel = "Rendering step $stepCount…",
                onClientEvent = { event ->
                    if (event is A2uiClientEventMessage) {
                        sendTurn(event.describeForGemini())
                    }
                },
            )

            else -> Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enter a starting prompt above and tap \"Start\". Any Button you tap in " +
                        "the generated UI will automatically continue the conversation with Gemini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Turns a tapped-button event into a plain-text description Gemini can react to as the next turn. */
private fun A2uiClientEventMessage.describeForGemini(): String {
    val contextJson = runCatching { JSONObject(context).toString() }.getOrDefault("{}")
    val dataModelJson = runCatching {
        clientDataModel?.toPayloadMap()?.let { JSONObject(it).toString() }
    }.getOrNull() ?: "{}"
    return "The user triggered event \"$type\" on component \"$componentId\". " +
        "Event context: $contextJson. Current data model values across all surfaces: " +
        "$dataModelJson. Continue the flow with the next step's UI."
}
