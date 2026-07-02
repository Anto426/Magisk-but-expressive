package com.topjohnwu.magisk.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.magisk.ui.component.card.MagiskCard
import com.topjohnwu.magisk.viewmodel.log.MagiskLogLevel
import com.topjohnwu.magisk.viewmodel.log.MagiskLogUiItem

@Composable
fun LogItem(item: MagiskLogUiItem) {
    val colors = MaterialTheme.colorScheme
    val expanded = true
    val levelColor = when (item.level) {
        MagiskLogLevel.VERBOSE -> colors.onSurfaceVariant
        MagiskLogLevel.DEBUG -> colors.primary
        MagiskLogLevel.INFO -> colors.tertiary
        MagiskLogLevel.WARN -> colors.secondary
        MagiskLogLevel.ERROR, MagiskLogLevel.FATAL -> colors.error
        else -> colors.onSurfaceVariant
    }

    MagiskCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        containerColor = if (item.level == MagiskLogLevel.ERROR || item.level == MagiskLogLevel.FATAL) {
            colors.errorContainer.copy(alpha = 0.26f)
        } else {
            colors.surfaceContainerHigh
        },
        border = null
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(if (expanded) 7.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.level.shortLabel,
                        color = levelColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = item.tag,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!expanded) {
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (expanded) {
                Text(
                    text = item.message,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.pid > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = levelColor.copy(alpha = 0.10f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = "PID ${item.pid}  TID ${item.tid}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else if (item.pid > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PID ${item.pid}  TID ${item.tid}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
