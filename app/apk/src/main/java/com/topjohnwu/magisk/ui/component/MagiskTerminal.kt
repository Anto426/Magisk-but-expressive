package com.topjohnwu.magisk.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.ui.motion.MagiskAutoScrollToLatest

@Composable
fun MagiskTerminal(
    lines: List<String>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    running: Boolean = false,
    emptyText: String? = null
) {
    MagiskAutoScrollToLatest(itemCount = lines.size, state = state, always = true)

    MagiskCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (lines.isEmpty() && emptyText != null) {
            MagiskInlineMessage(
                text = emptyText, icon = Icons.Rounded.Terminal, modifier = Modifier.padding(12.dp)
            )
        } else {
            val horizontalScrollState = rememberScrollState()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
                    .padding(10.dp),
                state = state,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(
                    items = lines,
                    key = { index, _ -> index },
                    contentType = { _, _ -> "terminal_line" }) { index, line ->
                    MagiskTerminalLine(
                        line = line,
                        isLastLine = index == lines.lastIndex,
                        running = running
                    )
                }
            }
        }
    }
}

@Composable
private fun MagiskTerminalLine(line: String, isLastLine: Boolean, running: Boolean) {
    val lineColor = terminalLineColor(line)
    val displayLine = remember(line) { line.withoutTerminalStatusPrefix() }
    val textStyle = TextStyle(
        color = lineColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.5.sp,
        lineHeight = 15.sp
    )
    val inverseForeground = MaterialTheme.colorScheme.surface

    var annotatedString = remember(displayLine, textStyle, inverseForeground) {
        displayLine.toTerminalAnnotatedString(
            baseStyle = textStyle,
            inverseForeground = inverseForeground
        )
    }

    if (isLastLine && running) {
        val transition = rememberInfiniteTransition(label = "cursor")
        val alpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cursor"
        )

        annotatedString = remember(annotatedString, alpha) {
            AnnotatedString.Builder(annotatedString).apply {
                pushStyle(SpanStyle(color = lineColor.copy(alpha = alpha)))
                append(" ▊")
                pop()
            }.toAnnotatedString()
        }
    }

    Text(
        text = annotatedString,
        modifier = Modifier,
        style = textStyle,
        maxLines = 1,
        softWrap = false
    )
}

@Composable
fun MagiskTerminalActions(
    modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun MagiskTerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    icon: @Composable (() -> Unit)? = null
) {
    val buttonModifier = modifier.height(MagiskComponentDefaults.ActionHeight)
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = MagiskComponentDefaults.ControlShape
        ) {
            if (icon != null) icon()
            Text(text = text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = MagiskComponentDefaults.ControlShape
        ) {
            if (icon != null) icon()
            Text(text = text)
        }
    }
}

@Composable
private fun terminalLineColor(line: String) = when {
    line.startsWith("!") -> MaterialTheme.colorScheme.error
    line.startsWith("-") -> MaterialTheme.colorScheme.primary
    line.startsWith("*") -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurface
}

private fun String.withoutTerminalStatusPrefix() = when {
    this == "-" -> ""
    startsWith("- ") -> drop(2)
    else -> this
}
