package com.topjohnwu.magisk.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object MagiskComponentDefaults {
    val ScreenHorizontalPadding: Dp = 16.dp
    val ScreenVerticalPadding: Dp = 12.dp
    val ScreenBottomPadding: Dp = 96.dp
    val SectionSpacing: Dp = 20.dp
    val ItemSpacing: Dp = 10.dp
    val DenseItemSpacing: Dp = 6.dp
    val ActionHeight: Dp = 48.dp
    val IconButtonSize: Dp = 40.dp
    val IconBadgeSize: Dp = 36.dp

    val CardShape: Shape = RoundedCornerShape(16.dp)
    val PanelShape: Shape = RoundedCornerShape(16.dp)
    val ControlShape: Shape = RoundedCornerShape(20.dp)
    val PillShape: Shape = RoundedCornerShape(50)

    val CardBorder: BorderStroke
        @Composable get() = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

    val CardContainer: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

    val PanelContainer: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest

    /** Main accent family used by interactive and decorative UI elements. */
    val AccentContainer: Color
        @Composable get() = MaterialTheme.colorScheme.primaryContainer

    val AccentContent: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer

    val SelectedContainer: Color
        @Composable get() = AccentContainer

    val SelectedContent: Color
        @Composable get() = AccentContent

    val DividerColor: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val PrimaryIconTint: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val SecondaryIconTint: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    val PrimaryText: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface

    val SecondaryText: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}
