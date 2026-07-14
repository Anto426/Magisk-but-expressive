package com.topjohnwu.magisk.ui.component.card

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.ui.component.MagiskComponentDefaults
import com.topjohnwu.magisk.ui.motion.MagiskMotionDuration
import com.topjohnwu.magisk.ui.motion.MotionCenter

@Composable
fun MagiskCard(
    modifier: Modifier = Modifier,
    shape: Shape = MagiskComponentDefaults.CardShape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = MagiskComponentDefaults.CardBorder,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleAnimation = MotionCenter.tweenSpec<Float>(MagiskMotionDuration.Short)
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && enabled && isPressed) {
            0.985f
        } else {
            1f
        },
        animationSpec = scaleAnimation,
        label = "MagiskCardScale"
    )

    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = containerColor.copy(alpha = 0.48f),
        disabledContentColor = contentColor.copy(alpha = 0.38f)
    )

    val elevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp,
        pressedElevation = 1.dp,
        focusedElevation = 1.dp,
        hoveredElevation = 1.dp,
        draggedElevation = 3.dp,
        disabledElevation = 0.dp
    )

    val cardModifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
            interactionSource = interactionSource
        ) {
            MagiskCardContent(
                contentPadding = contentPadding,
                content = content
            )
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation
        ) {
            MagiskCardContent(
                contentPadding = contentPadding,
                content = content
            )
        }
    }
}
