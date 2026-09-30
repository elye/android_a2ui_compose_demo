package com.example.a2uilibrarydemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.a2uilibrarydemo.a2ui.A2uiDemoScreen
import com.example.a2uilibrarydemo.a2ui.DemoPrompt
import com.example.a2uilibrarydemo.a2ui.demoPromptsByCategory
import com.example.a2uilibrarydemo.ai.AiConversationScreen
import com.example.a2uilibrarydemo.ai.AiPromptScreen
import com.example.a2uilibrarydemo.ai.GeminiSettingsScreen
import com.example.a2uilibrarydemo.ui.theme.A2UILibraryDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            A2UILibraryDemoTheme {
                A2uiDemoApp()
            }
        }
    }
}

/** The app's top-level destinations, selectable from the [HomeScreen]. */
private enum class TopScreen {
    /** Landing screen: pick between the three modes below. */
    HOME,

    /** The original tutorial: a picker of hand-written A2UI JSON-Lines scripts, one per component. */
    CATALOG,

    /** One-off demo: a free-text prompt is sent to Gemini, which writes the A2UI script itself. */
    AI_PROMPT,

    /** Multi-step demo: prompt -> rendered UI -> user interacts -> conversation continues. */
    AI_CONVERSATION,

    /** Where the user pastes their Gemini API key, used by [AI_PROMPT] and [AI_CONVERSATION]. */
    SETTINGS,
}

/**
 * Root composable for the A2UI tutorial app. Shows a [HomeScreen] with three entry points:
 * - **Catalog Demo**: the original tutorial, a selectable list of hand-written A2UI prompts, one
 *   per Basic Catalog V1 component, rendered directly by [A2uiDemoScreen].
 * - **AI Prompt Demo**: a free-text prompt is sent to the Gemini API (see [AiPromptScreen]), which
 *   writes an A2UI JSON-Lines script on the fly; the result is rendered the exact same way.
 * - **Multi-Step AI Demo**: like AI Prompt Demo, but the conversation continues — tapping a
 *   `Button` in the generated UI (see [AiConversationScreen]) reports the interaction back to
 *   Gemini, which responds with the next step's UI.
 *
 * A [TopScreen.SETTINGS] destination (reachable from the home screen's toolbar action, or from
 * either AI screen directly) lets the user paste in their own Gemini API key.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun A2uiDemoApp(modifier: Modifier = Modifier) {
    var topScreen by rememberSaveable { mutableStateOf(TopScreen.HOME) }
    var selectedPrompt by rememberSaveable(stateSaver = DemoPromptIdSaver) {
        mutableStateOf<DemoPrompt?>(null)
    }

    val title = when (topScreen) {
        TopScreen.HOME -> "A2UI Library Demo"
        TopScreen.CATALOG -> selectedPrompt?.title ?: "Catalog Demo"
        TopScreen.AI_PROMPT -> "AI Prompt Demo"
        TopScreen.AI_CONVERSATION -> "Multi-Step AI Demo"
        TopScreen.SETTINGS -> "Settings"
    }

    // Simple back behavior: drill back one level at a time rather than a full nav stack, since
    // this app is only ever two levels deep (top screen -> optional catalog prompt detail).
    fun goBack() {
        when (topScreen) {
            TopScreen.CATALOG -> if (selectedPrompt != null) {
                selectedPrompt = null
            } else {
                topScreen = TopScreen.HOME
            }
            TopScreen.AI_PROMPT, TopScreen.AI_CONVERSATION, TopScreen.SETTINGS -> topScreen = TopScreen.HOME
            TopScreen.HOME -> Unit
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (topScreen != TopScreen.HOME) {
                        IconButton(onClick = { goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (topScreen == TopScreen.HOME) {
                        IconButton(onClick = { topScreen = TopScreen.SETTINGS }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Gemini API key settings")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (topScreen) {
            TopScreen.HOME -> HomeScreen(
                onCatalogSelected = { topScreen = TopScreen.CATALOG },
                onAiPromptSelected = { topScreen = TopScreen.AI_PROMPT },
                onAiConversationSelected = { topScreen = TopScreen.AI_CONVERSATION },
                modifier = Modifier.padding(innerPadding),
            )

            TopScreen.CATALOG -> {
                val prompt = selectedPrompt
                if (prompt == null) {
                    DemoPromptList(
                        onPromptSelected = { selectedPrompt = it },
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    A2uiDemoScreen(prompt = prompt, modifier = Modifier.padding(innerPadding))
                }
            }

            TopScreen.AI_PROMPT -> AiPromptScreen(
                onOpenSettings = { topScreen = TopScreen.SETTINGS },
                modifier = Modifier.padding(innerPadding),
            )

            TopScreen.AI_CONVERSATION -> AiConversationScreen(
                onOpenSettings = { topScreen = TopScreen.SETTINGS },
                modifier = Modifier.padding(innerPadding),
            )

            TopScreen.SETTINGS -> GeminiSettingsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

/** Landing screen: lets the user pick between the base catalog render and the two AI-driven demos. */
@Composable
private fun HomeScreen(
    onCatalogSelected: () -> Unit,
    onAiPromptSelected: () -> Unit,
    onAiConversationSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text(
                text = "Choose a demo mode",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onCatalogSelected)) {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.ViewQuilt, contentDescription = null) },
                    headlineContent = { Text("Catalog Demo") },
                    supportingContent = {
                        Text("Browse hand-written A2UI scripts, one per Basic Catalog V1 component.")
                    },
                )
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable(onClick = onAiPromptSelected),
            ) {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    headlineContent = { Text("AI Prompt Demo") },
                    supportingContent = {
                        Text("Describe a UI in your own words and have Gemini generate + render it live.")
                    },
                )
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable(onClick = onAiConversationSelected),
            ) {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Forum, contentDescription = null) },
                    headlineContent = { Text("Multi-Step AI Demo") },
                    supportingContent = {
                        Text("Prompt Gemini, tap a button in the result, and watch the conversation continue.")
                    },
                )
            }
        }
    }
}

/** Lists every [DemoPrompt], grouped by category, so the user can pick a component to preview. */
@Composable
private fun DemoPromptList(
    onPromptSelected: (DemoPrompt) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        demoPromptsByCategory.forEach { (category, prompts) ->
            item(key = "header-$category") {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(prompts, key = { it.id }) { prompt ->
                ListItem(
                    headlineContent = { Text(prompt.title) },
                    supportingContent = { Text(prompt.description) },
                    modifier = Modifier
                        .clickable { onPromptSelected(prompt) },
                )
                HorizontalDivider()
            }
        }
    }
}

private val DemoPromptIdSaver = androidx.compose.runtime.saveable.Saver<DemoPrompt?, String>(
    save = { it?.id ?: "" },
    restore = { id -> com.example.a2uilibrarydemo.a2ui.demoPrompts.firstOrNull { it.id == id } },
)

@Preview(showBackground = true)
@Composable
fun A2uiDemoAppPreview() {
    A2UILibraryDemoTheme {
        A2uiDemoApp()
    }
}