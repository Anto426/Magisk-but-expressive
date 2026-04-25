package com.topjohnwu.magisk.ui.animation

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ExpandableCardMotion(
    val elevation: Dp,
    val rotation: Float,
    val scale: Float
)

@Composable
fun rememberExpandableCardMotion(
    expanded: Boolean,
    expandedElevation: Dp = 10.dp,
    collapsedElevation: Dp = 2.dp,
    expandedScale: Float = 1f,
    collapsedScale: Float = 0.992f,
    expandedRotation: Float = 90f,
    collapsedRotation: Float = 0f
): ExpandableCardMotion {
    val transition = updateTransition(targetState = expanded, label = "expandableCardTransition")
    val elevation by transition.animateDp(
        transitionSpec = {
            MagiskMotion.noBounceSpring(stiffness = MotionTokens.StiffnessMediumLow)
        },
        label = "elevation"
    ) { if (it) expandedElevation else collapsedElevation }
    val rotation by transition.animateFloat(
        transitionSpec = {
            MagiskMotion.lowBounceSpring()
        },
        label = "rotation"
    ) { if (it) expandedRotation else collapsedRotation }
    val scale by transition.animateFloat(
        transitionSpec = {
            MagiskMotion.noBounceSpring(stiffness = MotionTokens.StiffnessMediumLow)
        },
        label = "scale"
    ) { if (it) expandedScale else collapsedScale }

    return ExpandableCardMotion(elevation, rotation, scale)
}
