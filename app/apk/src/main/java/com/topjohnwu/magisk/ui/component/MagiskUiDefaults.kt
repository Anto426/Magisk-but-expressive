package com.topjohnwu.magisk.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object MagiskUiDefaults {
    val ScreenHorizontalPadding: Dp = 20.dp
    val ScreenTopPadding: Dp = 16.dp
    val ScreenBottomPadding: Dp = 140.dp
    val SnackbarBottomPadding: Dp = 110.dp
    val SnackbarBottomPaddingWithBar: Dp = 120.dp
    val FloatingActionBottomPaddingWithBar: Dp = 132.dp

    val ListItemSpacing: Dp = 24.dp
    val SectionSpacing: Dp = 16.dp
    val DenseItemSpacing: Dp = 16.dp

    val ActionHeight: Dp = 56.dp
    val PrimaryActionHeight: Dp = 64.dp
    val IconActionSize: Dp = 52.dp
    val BottomBarHeight: Dp = 88.dp
    val CardElevation: Dp = 2.dp
    val ExpandedCardElevation: Dp = 10.dp
    val FloatingBarTonalElevation: Dp = 8.dp
    val FloatingBarShadowElevation: Dp = 12.dp

    val IconContainerSize: Dp = 48.dp
    val SmallIconContainerSize: Dp = 32.dp

    val SmallShape: Shape = RoundedCornerShape(16.dp)
    val MediumShape: Shape = RoundedCornerShape(20.dp)
    val LargeShape: Shape = RoundedCornerShape(24.dp)
    val ExtraLargeShape: Shape = RoundedCornerShape(32.dp)
    val OrganicShape: Shape = RoundedCornerShape(
        topStart = 48.dp,
        bottomEnd = 48.dp,
        topEnd = 16.dp,
        bottomStart = 16.dp
    )
    val OrganicShapeReversed: Shape = RoundedCornerShape(
        topStart = 16.dp,
        bottomEnd = 16.dp,
        topEnd = 48.dp,
        bottomStart = 48.dp
    )
    val HeroShape: Shape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 80.dp,
        bottomStart = 80.dp,
        bottomEnd = 32.dp
    )
    val PillShape: Shape = CircleShape

    fun screenContentPadding(
        top: Dp = ScreenTopPadding,
        bottom: Dp = ScreenBottomPadding,
        horizontal: Dp = ScreenHorizontalPadding
    ): PaddingValues = PaddingValues(
        start = horizontal,
        end = horizontal,
        top = top,
        bottom = bottom
    )

    fun verticalContentPadding(
        top: Dp = ScreenTopPadding,
        bottom: Dp = ScreenBottomPadding
    ): PaddingValues = PaddingValues(
        top = top,
        bottom = bottom
    )
}
