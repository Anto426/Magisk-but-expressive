package com.topjohnwu.magisk.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.magisk.ui.animation.MagiskMotion
import com.topjohnwu.magisk.ui.terminal.StyledLogLine
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun TerminalScreenScaffold(
    lines: List<String>,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    emptyText: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    LaunchedEffect(lines.size) {
        val last = lines.lastIndex
        if (last >= 0) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val shouldStickToBottom = lastVisible >= last - 3 || !listState.canScrollForward
            if (shouldStickToBottom) {
                listState.scrollToItem(last)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MagiskUiDefaults.ScreenHorizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(MagiskUiDefaults.ListItemSpacing)
        ) {
            TerminalLogContainer(
                lines = lines,
                listState = listState,
                emptyText = emptyText,
                modifier = Modifier.weight(1f)
            )

            TerminalActionRow(content = actions)
        }

        MagiskSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MagiskUiDefaults.SnackbarBottomPadding)
        )
    }
}

@Composable
fun TerminalLogContainer(
    lines: List<String>,
    listState: LazyListState,
    emptyText: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Rounded.Terminal,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(CoreR.string.logs),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxSize(),
            shape = MagiskUiDefaults.LargeShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (lines.isEmpty()) {
                    Text(
                        text = emptyText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(
                            items = lines,
                            key = { index, _ -> index }
                        ) { _, line ->
                            StyledLogLine(
                                line = line,
                                colors = MaterialTheme.colorScheme,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun TerminalSaveLogButton(
    hasLogs: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = hasLogs,
        modifier = modifier.height(MagiskUiDefaults.ActionHeight),
        shape = MagiskUiDefaults.SmallShape,
        border = ButtonDefaults.outlinedButtonBorder(hasLogs).copy(width = 1.dp)
    ) {
        Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            androidx.compose.ui.res.stringResource(CoreR.string.menuSaveLog),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TerminalRunningActionSlot(
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    finished: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = isRunning,
        modifier = modifier,
        transitionSpec = { MagiskMotion.fadeContent() },
        label = "terminalAction"
    ) { running ->
        if (running) {
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MagiskUiDefaults.ActionHeight),
                shape = MagiskUiDefaults.SmallShape
            ) {
                Text(
                    androidx.compose.ui.res.stringResource(CoreR.string.running),
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            finished()
        }
    }
}

@Composable
fun TerminalCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(MagiskUiDefaults.ActionHeight),
        shape = MagiskUiDefaults.SmallShape
    ) {
        Text(
            androidx.compose.ui.res.stringResource(CoreR.string.close),
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun TerminalRebootButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(MagiskUiDefaults.ActionHeight),
        shape = MagiskUiDefaults.SmallShape,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.Rounded.RestartAlt, null)
        Spacer(Modifier.width(8.dp))
        Text(
            androidx.compose.ui.res.stringResource(CoreR.string.reboot),
            fontWeight = FontWeight.Black
        )
    }
}
