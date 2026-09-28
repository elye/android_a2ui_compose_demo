package com.example.a2uilibrarydemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.a2uilibrarydemo.a2ui.A2uiDemoScreen
import com.example.a2uilibrarydemo.a2ui.DemoPrompt
import com.example.a2uilibrarydemo.a2ui.demoPromptsByCategory
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

/**
 * Root composable for the A2UI tutorial app: shows a selectable list of demo prompts, one per
 * A2UI Basic Catalog V1 component, and switches to a live-rendered [A2uiDemoScreen] on selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun A2uiDemoApp(modifier: Modifier = Modifier) {
    var selectedPrompt by rememberSaveable(stateSaver = DemoPromptIdSaver) {
        mutableStateOf<DemoPrompt?>(null)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(selectedPrompt?.title ?: "A2UI Component Tutorial") },
                navigationIcon = {
                    if (selectedPrompt != null) {
                        IconButton(onClick = { selectedPrompt = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
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