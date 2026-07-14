package com.topjohnwu.magisk.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.topjohnwu.magisk.core.R as CoreR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MagiskLoadingTransition(
    val generation: Long = 0,
    val active: Boolean = false
)

/**
 * Coordinates short, app-wide UI transitions such as applying a theme or locale.
 * A generation token prevents an older completion from hiding a newer transition.
 */
object MagiskLoadingController {
    private val _transition = MutableStateFlow(MagiskLoadingTransition())
    val transition: StateFlow<MagiskLoadingTransition> = _transition.asStateFlow()

    fun begin(): Long {
        var generation = 0L
        _transition.update { current ->
            generation = current.generation + 1
            MagiskLoadingTransition(generation = generation, active = true)
        }
        return generation
    }

    fun complete(generation: Long) {
        _transition.update { current ->
            if (current.active && current.generation == generation) {
                current.copy(active = false)
            } else {
                current
            }
        }
    }
}

/** A single loader for full-page and inline loading states. */
@Composable
fun MagiskLoader(
    modifier: Modifier = Modifier,
    message: String? = null,
    inline: Boolean = false
) {
    val loadingText = message ?: stringResource(CoreR.string.loading)
    val semanticsModifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = loadingText
        liveRegion = LiveRegionMode.Polite
    }

    if (inline) {
        Row(
            modifier = modifier.then(semanticsModifier),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp)
                .then(semanticsModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = 14.dp,
                alignment = Alignment.CenterVertically
            )
        ) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
            Text(
                text = loadingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Blocks interaction while an app-wide visual transition is being committed. */
@Composable
fun MagiskLoadingOverlay(
    modifier: Modifier = Modifier,
    message: String = stringResource(CoreR.string.loading)
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .zIndex(Float.MAX_VALUE)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.widthIn(min = 180.dp, max = 280.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                MagiskLoader(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    message = message,
                    inline = true
                )
            }
        }
    }
}
