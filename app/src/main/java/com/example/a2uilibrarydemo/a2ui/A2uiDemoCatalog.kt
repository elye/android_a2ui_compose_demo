package com.example.a2uilibrarydemo.a2ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.catalog.functions.A2uiMessageFormatter
import androidx.a2ui.model.catalog.functions.A2uiUrlOpener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Builds the A2UI Basic Catalog V1 [A2uiCatalog] used by the whole demo app.
 *
 * `materialA2uiBasicCatalogV1` requires an explicit [A2uiBasicCatalogV1.Image],
 * [A2uiBasicCatalogV1.Video], [A2uiBasicCatalogV1.AudioPlayer], [A2uiUrlOpener], and
 * [A2uiMessageFormatter] — there are no built-in defaults for those five (unlike every other
 * component, which Material3 already provides a sensible default for). This app has no
 * third-party image/video loading library, so minimal, dependency-free implementations of the
 * catalog component interfaces are provided directly here.
 */
fun buildDemoCatalog(): A2uiCatalog = materialA2uiBasicCatalogV1(
    image = DemoImage,
    video = DemoVideo,
    audioPlayer = DemoAudioPlayer,
    urlOpener = DemoUrlOpener,
    messageFormatter = DemoMessageFormatter,
    localeProvider = A2uiLocaleProvider.Default,
)

/** Loads and renders a remote image with [android.graphics.BitmapFactory] (no extra deps). */
private object DemoImage : A2uiBasicCatalogV1.Image {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        description: String?,
        fit: A2uiBasicCatalogV1.Image.Fit,
        variant: A2uiBasicCatalogV1.Image.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
        LaunchedEffect(url) {
            runCatching {
                withContext(Dispatchers.IO) {
                    URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                }
            }.onSuccess { bitmap = it }
        }
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = description,
                modifier = modifier,
            )
        } else {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

/** Video playback is not implemented for this tutorial; shows a URL placeholder instead. */
private object DemoVideo : A2uiBasicCatalogV1.Video {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Video placeholder:\n$url")
        }
    }
}

/** Audio playback is not implemented for this tutorial; shows a URL placeholder instead. */
private object DemoAudioPlayer : A2uiBasicCatalogV1.AudioPlayer {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        description: String?,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Audio placeholder: ${description.orEmpty()}\n$url")
        }
    }
}

/** No-op URL opener for this tutorial (a real app would launch an Intent/Browser). */
private object DemoUrlOpener : A2uiUrlOpener {
    override fun openUrl(url: String) {
        // Intentionally a no-op for this tutorial demo.
    }
}

/** Trivial passthrough message formatter (no ICU pluralization needed for this tutorial). */
private object DemoMessageFormatter : A2uiMessageFormatter {
    override fun format(
        pattern: String,
        locale: java.util.Locale,
        arguments: Map<String, Any>,
    ): String = pattern
}
