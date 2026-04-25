package com.topjohnwu.magisk.ui.log

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.magisk.ui.animation.MagiskMotion
import com.topjohnwu.magisk.ui.component.MagiskCard
import com.topjohnwu.magisk.ui.component.MagiskUiDefaults
import com.topjohnwu.magisk.ui.terminal.ansiLogText
import com.topjohnwu.magisk.core.R as CoreR

@Composable
internal fun LogHeroCard(
    stats: LogStats,
    loading: Boolean,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    MagiskCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MagiskUiDefaults.HeroShape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = CoreR.string.log_entries).lowercase() + ": ${stats.total}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LogMetricTile(stringResource(id = CoreR.string.log_issues), stats.issues.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.error)
                LogMetricTile(stringResource(id = CoreR.string.log_sources), stats.sources.toString(), Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LogActionBtn(Icons.Rounded.Refresh, onRefresh, loading)
                LogActionBtn(Icons.Rounded.SaveAlt, onSave)
                LogActionBtn(Icons.Rounded.DeleteSweep, onClear, isDestructive = true)
            }
        }
    }
}

@Composable
private fun LogMetricTile(label: String, value: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onPrimaryContainer) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = MagiskUiDefaults.MediumShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    ) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun LogActionBtn(icon: ImageVector, onClick: () -> Unit, loading: Boolean = false, isDestructive: Boolean = false) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Icon(icon, null, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun LogFilterSection(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilter: LogDisplayFilter,
    onFilterChange: (LogDisplayFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(id = CoreR.string.log_search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            shape = CircleShape,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LogDisplayFilter.entries.forEach {
                FilterChip(
                    selected = it == activeFilter,
                    onClick = { onFilterChange(it) },
                    label = { Text(stringResource(id = it.labelRes)) },
                    shape = CircleShape
                )
            }
        }
    }
}

@Composable
internal fun LogEventCard(item: MagiskLogUiItem) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val accent = item.level.color()

    MagiskCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(MagiskMotion.cardContentSpring()),
        shape = MagiskUiDefaults.LargeShape,
        containerColor = if (item.isIssue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = accent.copy(alpha = 0.2f), contentColor = accent) {
                    Text(item.level.shortLabel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
                Text(item.tag.ifBlank { item.sourceLabel }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text(item.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MagiskUiDefaults.MediumShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.1f))
            ) {
                SelectionContainer {
                    Text(
                        text = ansiLogText(item.message, MaterialTheme.colorScheme),
                        modifier = Modifier.padding(12.dp).clickable { expanded = !expanded },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = if (expanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
