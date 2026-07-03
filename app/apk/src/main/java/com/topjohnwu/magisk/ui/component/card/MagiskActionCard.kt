package com.topjohnwu.magisk.ui.component.card

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MagiskActionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 72.dp,
    content: @Composable RowScope.() -> Unit
) {
    MagiskCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            content = content
        )
    }
}
