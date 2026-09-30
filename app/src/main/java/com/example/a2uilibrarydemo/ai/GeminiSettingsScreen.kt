package com.example.a2uilibrarydemo.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Lets the user paste in their own Gemini API key (from [Google AI Studio](https://aistudio.google.com/apikey))
 * so the "AI Prompt" demo screen can call the Gemini API on their behalf. Persisted locally via
 * [GeminiApiKeyStore] — never sent anywhere except directly to the Gemini API itself.
 */
@Composable
fun GeminiSettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var apiKey by rememberSaveable { mutableStateOf(GeminiApiKeyStore.getApiKey(context)) }
    var isVisible by remember { mutableStateOf(false) }
    var savedJustNow by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Gemini API Key",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Get a free key from Google AI Studio (aistudio.google.com/apikey) and paste it " +
                "below. It's stored only on this device and used solely to call the Gemini API " +
                "from the \"AI Prompt\" demo screen.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                savedJustNow = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API key") },
            singleLine = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            trailingIcon = {
                IconButton(onClick = { isVisible = !isVisible }) {
                    Icon(
                        imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (isVisible) "Hide API key" else "Show API key",
                    )
                }
            },
        )

        Button(
            onClick = {
                GeminiApiKeyStore.setApiKey(context, apiKey)
                savedJustNow = true
            },
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text("Save")
        }

        if (savedJustNow) {
            Text(
                text = "Saved.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GeminiSettingsScreenPreview() {
    GeminiSettingsScreen()
}
