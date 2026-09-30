package com.example.a2uilibrarydemo.ai

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.a2uilibrarydemo.a2ui.A2uiMessagesRenderer
import kotlinx.coroutines.launch

/**
 * Starter prompts shown as quick-tap suggestions above the free-text field. Collectively these
 * exercise every A2UI Basic Catalog V1 component (Text, Image, Icon, Video, AudioPlayer, Row,
 * Column, List, Card, Tabs, Modal, Divider, Button, TextField, CheckBox, ChoicePicker, Slider,
 * DateTimeInput) so a user can explore the full catalog without typing a prompt themselves.
 */
private val SUGGESTED_PROMPTS = listOf(
    "Sign-up form" to "A simple sign-up form with a name field, email field, and a primary submit button.",
    "Settings screen" to "A settings screen with a dark mode checkbox, a volume slider, and a notification frequency choice picker.",
    "Product card" to "A product card with an image, title, price, and an 'Add to cart' button.",
    "Media player" to "A media player screen with a video player, an audio player below it, and a caption describing each.",
    "Icon toolbar" to "A toolbar row of icon buttons (favorite, share, settings, home) separated by a divider from a caption below.",
    "Profile card" to "A profile card showing an avatar image, a name, an icon-labelled row of stats, and a 'Follow' button.",
    "Event booking" to "An event booking form with a date/time picker for the event date, a text field for attendee name, a choice picker for ticket type, and a 'Book now' button that opens a confirmation modal.",
    "Login tabs" to "A login screen using tabs to switch between 'Sign in' (email + password fields) and 'Sign up' (name + email + password fields), each with its own submit button.",
    "Trip itinerary" to "A trip itinerary using tabs for 'Day 1' and 'Day 2', each showing a short list of activities.",
)

/**
 * The "AI Prompt" demo screen: lets the user type a free-text request, sends it to the Gemini API
 * along with a system prompt that teaches the model the A2UI protocol contract, and renders
 * whatever A2UI JSON-Lines script comes back using the same [A2uiMessagesRenderer] the hand-written
 * catalog demos use — a real end-to-end "AI prompt -> live rendered UI" round trip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPromptScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var promptText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var generatedMessages by remember { mutableStateOf<List<String>?>(null) }
    // Bumped on every successful generation so A2uiMessagesRenderer always starts from a clean
    // processor/parser, even if the exact same script is generated twice in a row.
    var generationId by remember { mutableIntStateOf(0) }

    fun generate() {
        val apiKey = GeminiApiKeyStore.getApiKey(context)
        if (apiKey.isBlank()) {
            errorMessage = "No Gemini API key set. Tap the settings icon to add one."
            return
        }
        if (promptText.isBlank()) return

        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            runCatching { generateA2uiScript(apiKey = apiKey, userPrompt = promptText) }
                .onSuccess { messages ->
                    generatedMessages = messages
                    generationId += 1
                }
                .onFailure { throwable ->
                    errorMessage = throwable.message ?: "Unknown error generating UI."
                }
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Describe the UI you want",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text(" API key")
                }
            }

            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text("Prompt") },
                placeholder = { Text("e.g. A login form with email, password, and a submit button") },
                minLines = 3,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SUGGESTED_PROMPTS.forEach { (label, suggestion) ->
                    OutlinedButton(onClick = { promptText = suggestion }) {
                        Text(label)
                    }
                }
            }

            Button(
                onClick = { generate() },
                enabled = !isLoading && promptText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(if (isLoading) "Generating…" else "Generate UI")
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
                loadingLabel = "Rendering generated UI…",
            )

            else -> Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enter a prompt above and tap \"Generate UI\" to have Gemini write an " +
                        "A2UI protocol script and render it live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
