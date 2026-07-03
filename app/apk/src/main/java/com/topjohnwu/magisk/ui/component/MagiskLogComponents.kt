package com.topjohnwu.magisk.ui.component

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.magisk.viewmodel.log.MagiskLogLevel
import com.topjohnwu.magisk.viewmodel.log.MagiskLogUiItem
import com.topjohnwu.magisk.view.SystemToastManager
import kotlinx.coroutines.launch
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun LogItem(item: MagiskLogUiItem) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember(item.id) { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val copyText = remember(item) { item.toClipboardText() }
    val levelColor = item.level.toColor()
    val isIssue = item.level == MagiskLogLevel.ERROR || item.level == MagiskLogLevel.FATAL

    MagiskExpandableListItem(
        title = item.tag.ifBlank { item.sourceLabel },
        subtitle = if (expanded) item.timestamp else item.message,
        expanded = expanded,
        onClick = { expanded = !expanded },
        containerColor = if (isIssue) {
            colors.errorContainer
        } else {
            MagiskComponentDefaults.CardContainer
        },
        border = if (isIssue) {
            BorderStroke(1.dp, colors.error.copy(alpha = 0.45f))
        } else {
            MagiskComponentDefaults.CardBorder
        },
        leadingContent = {
            LogLevelBadge(
                label = item.level.shortLabel,
                color = levelColor
            )
        },
        headerTrailingContent = {
            Text(
                text = item.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    ) {
        LogExpandedContent(
            item = item,
            levelColor = levelColor,
            onCopy = {
                coroutineScope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("log", copyText))
                    )
                    SystemToastManager.show(
                        context,
                        context.getString(CoreR.string.copied_to_clipboard)
                    )
                }
            }
        )
    }
}

@Composable
private fun MagiskLogLevel.toColor(): Color {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        MagiskLogLevel.VERBOSE -> colors.onSurfaceVariant
        MagiskLogLevel.DEBUG -> colors.primary
        MagiskLogLevel.INFO -> colors.tertiary
        MagiskLogLevel.WARN -> colors.secondary
        MagiskLogLevel.ERROR,
        MagiskLogLevel.FATAL -> colors.error

        else -> colors.onSurfaceVariant
    }
}

@Composable
private fun LogLevelBadge(
    label: String,
    color: Color,
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LogExpandedContent(
    item: MagiskLogUiItem,
    levelColor: Color,
    onCopy: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectionContainer {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = item.message,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogMetaChip(text = item.level.name, color = levelColor)
            LogMetaChip(text = item.timestamp)
            if (item.pid > 0) {
                LogMetaChip(text = "PID ${item.pid}")
                LogMetaChip(text = "TID ${item.tid}")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onCopy,
                shape = MagiskComponentDefaults.ControlShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(CoreR.string.copy_log_entry))
            }
        }
    }
}

@Composable
private fun LogMetaChip(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f),
        contentColor = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun MagiskLogUiItem.toClipboardText() = buildString {
    append(timestamp)
    append(' ')
    append(level.shortLabel)
    if (tag.isNotBlank()) {
        append('/')
        append(tag)
    }
    if (pid > 0) {
        append(" PID ")
        append(pid)
        append(" TID ")
        append(tid)
    }
    append('\n')
    append(raw.ifBlank { message })
}
