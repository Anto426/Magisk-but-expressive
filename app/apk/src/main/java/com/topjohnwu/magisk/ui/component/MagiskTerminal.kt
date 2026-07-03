package com.topjohnwu.magisk.ui.component

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    emptyText: String? = null
) {
    MagiskAutoScrollToLatest(itemCount = lines.size, state = state)

    MagiskCard(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (lines.isEmpty() && emptyText != null) {
            MagiskInlineMessage(
                text = emptyText, icon = Icons.Rounded.Terminal, modifier = Modifier.padding(12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                state = state,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(
                    items = lines,
                    key = { index, _ -> index },
                    contentType = { _, _ -> "terminal_line" }) { _, line ->
                    MagiskTerminalLine(line)
                }
            }
        }
    }
}

@Composable
private fun MagiskTerminalLine(line: String) {
    val lineColor = terminalLineColor(line)
    val displayLine = remember(line) { line.withoutTerminalStatusPrefix() }
    val textStyle = MaterialTheme.typography.bodySmall.copy(
        color = lineColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 13.sp
    )
    val inverseForeground = MaterialTheme.colorScheme.surface
    val text = remember(displayLine, textStyle, inverseForeground) {
        displayLine.toTerminalAnnotatedString(
            baseStyle = textStyle,
            inverseForeground = inverseForeground
        )
    }

    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = textStyle,
        maxLines = 8,
        overflow = TextOverflow.Ellipsis
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
    else -> MaterialTheme.colorScheme.onSurface
}

private fun String.withoutTerminalStatusPrefix() = when {
    this == "-" -> ""
    startsWith("- ") -> drop(2)
    else -> this
}
